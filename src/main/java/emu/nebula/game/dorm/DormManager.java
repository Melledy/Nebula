package emu.nebula.game.dorm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;

import emu.nebula.GameConstants;
import emu.nebula.Nebula;
import emu.nebula.data.GameData;
import emu.nebula.data.resources.DormRoomDef;
import emu.nebula.data.resources.DropPkgDef;
import emu.nebula.database.GameDatabaseObject;
import emu.nebula.game.character.GameCharacter;
import emu.nebula.game.inventory.ItemParamMap;
import emu.nebula.game.player.Player;
import emu.nebula.game.player.PlayerChangeInfo;
import emu.nebula.game.player.PlayerManager;
import emu.nebula.game.quest.QuestCondition;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.DormInfo.DormInfoListResp;
import emu.nebula.proto.Public;
import emu.nebula.proto.PublicDorm.AchievementComplete;
import emu.nebula.proto.PublicDorm.AchievementInfo;
import emu.nebula.proto.PublicDorm.DormAffinityGiftInfo;
import emu.nebula.proto.PublicDorm.DormHot;
import emu.nebula.proto.PublicDorm.DormInfoData;
import emu.nebula.proto.PublicDorm.DormRoomUnlock;
import emu.nebula.proto.PublicDorm.RoleInteraction;
import emu.nebula.util.Utils;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

import lombok.Getter;

@Getter
@Entity(value = "dorm", useDiscriminator = false)
public class DormManager extends PlayerManager implements GameDatabaseObject {
    @Id
    private int uid;

    // Dorm details
    private String dormName;
    private long dormNameRestTime;
    
    // Furniture collection
    private Int2IntMap furnitureCollection;

    // Saved room state
    private Map<Integer, DormRoomState> rooms;
    private Map<Integer, DormPreset> presets;

    // Rewards / achievements
    private IntSet receivedLayoutRewardIds;
    private IntSet receivedRewardIds;
    private Map<Integer, Long> completedAchievements;

    // Social stats
    private int likeNum;
    private int dayVisitNum;
    private int visitTotalNum;

    // Daily state
    private long lastResetDay;
    private IntSet likedFriendUids;
    private IntSet dailyVisitorUids;
    private IntSet dailyInteractionRoleIds;
    private Int2IntMap dailyAffinityGiftCounts;
    private int dailyAffinityGiftTotal;

    private transient boolean queueSave;

    @Deprecated // Morphia only
    public DormManager() {
        
    }

    public DormManager(Player player) {
        super(player);

        this.uid = player.getUid();
        this.dormName = "";
        this.furnitureCollection = new Int2IntOpenHashMap();
        this.presets = new HashMap<>();
        this.rooms = new HashMap<>();
        this.receivedLayoutRewardIds = new IntOpenHashSet();
        this.receivedRewardIds = new IntOpenHashSet();
        this.completedAchievements = new HashMap<>();
        this.likedFriendUids = new IntOpenHashSet();
        this.dailyVisitorUids = new IntOpenHashSet();
        this.dailyInteractionRoleIds = new IntOpenHashSet();
        this.dailyAffinityGiftCounts = new Int2IntOpenHashMap();
        this.lastResetDay = Utils.getResetEpochDay();

        this.save();
    }
    
    public int getState() {
        return this.getReceivedRewardIds().isEmpty() ? 1 : 0;
    }

    // Daily reset

    public synchronized void resetDailies() {
        long currentDay = Utils.getResetEpochDay();

        if (currentDay <= this.lastResetDay) {
            return;
        }

        this.likedFriendUids.clear();
        this.dailyVisitorUids.clear();
        this.dailyInteractionRoleIds.clear();
        this.dailyAffinityGiftCounts.clear();
        this.dailyAffinityGiftTotal = 0;
        this.dayVisitNum = 0;
        this.lastResetDay = currentDay;
        this.queueSave = true;
    }

    // Rooms

    /**
     * Returns the ids of all rooms the player has access to. Rooms are unlocked
     * by acquiring the room item, which creates the room state in {@link #rooms}.
     */
    public IntSet getUnlockedRoomIds() {
        var result = new IntOpenHashSet();

        // Rooms that are available by default
        for (var room : GameData.getDormRoomDataTable()) {
            if (room.isDefaultCanUse()) {
                result.add(room.getId());
            }
        }

        // Rooms that have been unlocked by the player
        if (this.rooms != null) {
            result.addAll(this.rooms.keySet());
        }

        return result;
    }

    public boolean isRoomUnlocked(int roomId) {
        return this.getUnlockedRoomIds().contains(roomId);
    }

    public synchronized DormRoomState getOrCreateRoom(int roomId) {
        var room = this.rooms.get(roomId);
        if (room != null) {
            return room;
        }

        var data = GameData.getDormRoomDataTable().get(roomId);
        if (data == null) {
            return null;
        }

        room = new DormRoomState(roomId);
        room.setFurnitureList(createDefaultFurniture(data));

        this.rooms.put(roomId, room);
        this.queueSave = true;

        return room;
    }

    public synchronized boolean unlockRoom(int roomId, PlayerChangeInfo change) {
        if (!GameData.getDormRoomDataTable().containsKey(roomId)) {
            return false;
        }
        
        if (this.rooms.containsKey(roomId)) {
            return false;
        }

        this.getOrCreateRoom(roomId);

        if (change != null) {
            this.getPlayer().trigger(QuestCondition.DormUnlockRoom, 1);

            if (this.getPlayer().hasSession()) {
                var proto = DormRoomUnlock.newInstance()
                        .setDormRoomId(roomId)
                        .setChangeInfo(change.toProto());

                this.getPlayer().addNextPackage(NetMsgId.dorm_room_unlock_notify, proto);
            }
        }

        return true;
    }
    
    // Furniture
    
    /**
     * Adds furniture to the collection
     */
    public synchronized void addFurniture(int id, int amount) {
        this.furnitureCollection.put(id, this.furnitureCollection.get(id) + amount);
        this.queueSave = true;
    }
    
    /**
     * Returns how many of the given furniture the player owns
     */
    public int getFurnitureCount(int id) {
        if (this.furnitureCollection == null) {
            return 0;
        }
        
        return this.furnitureCollection.get(id);
    }
    
    /**
     * Returns true if the player owns at least one of the given furniture
     */
    public boolean hasFurniture(int id) {
        return this.getFurnitureCount(id) > 0;
    }

    public static List<DormFurniture> createDefaultFurniture(DormRoomDef room) {
        var result = new ArrayList<DormFurniture>();

        if (room == null || room.getDefaultLayoutId() <= 0) {
            return result;
        }

        var layoutReward = GameData.getDormLayoutRewardDataTable().get(room.getDefaultLayoutId());
        if (layoutReward == null) {
            return result;
        }

        int baseId = room.getDefaultLayoutId() * 100;

        for (int i = 1; i <= layoutReward.getFurnitureCount(); i++) {
            var layout = GameData.getDormRoomLayoutDataTable().get(baseId + i);
            if (layout == null || layout.getPos() == null || layout.getPos().length < 3) {
                continue;
            }

            int[] pos = layout.getPos();
            result.add(new DormFurniture(layout.getFurnitureId(), pos[0], pos[1], pos[2], layout.getRot()));
        }

        return result;
    }

    public static boolean isValidFurnitureList(List<DormFurniture> furnitureList) {
        if (furnitureList == null || furnitureList.size() > 1000) {
            return false;
        }

        for (var furniture : furnitureList) {
            if (furniture == null || furniture.getFurnitureId() <= 0) {
                return false;
            }
            if (!GameData.getDormFurnitureDataTable().containsKey(furniture.getFurnitureId())) {
                return false;
            }
        }

        return true;
    }

    public synchronized Long saveFurnitureLayout(int roomId, List<DormFurniture> furnitureList, int buildId) {
        if (!this.isRoomUnlocked(roomId)) {
            return null;
        }

        var roomData = GameData.getDormRoomDataTable().get(roomId);
        if (roomData == null || !isValidFurnitureList(furnitureList)) {
            return null;
        }

        long now = Nebula.getCurrentServerTime();

        if (buildId == 0) {
            var room = this.getOrCreateRoom(roomId);
            if (room == null) {
                return null;
            }

            room.setFurnitureList(new ArrayList<>(furnitureList));
            this.queueSave = true;

            this.getPlayer().trigger(QuestCondition.DormPlaceFurniture, furnitureList.size());
            this.getPlayer().trigger(QuestCondition.DormSaveRoom, 1);

            return 0L;
        }

        if (buildId < 1 || buildId > GameConstants.DORM_DEFAULT_PRESET_NUM) {
            return null;
        }

        var preset = this.presets.get(buildId);

        if (preset == null) {
            if (this.presets.size() >= GameConstants.DORM_DEFAULT_PRESET_NUM) {
                return null;
            }

            preset = new DormPreset(buildId, roomData.getRoomType(), now);
            this.presets.put(buildId, preset);
        } else {
            preset.setCreateTime(now);
            preset.setDormType(roomData.getRoomType());
        }

        preset.setFurnitureList(new ArrayList<>(furnitureList));
        this.queueSave = true;

        return now;
    }

    public synchronized boolean deletePreset(int buildId) {
        if (this.presets.remove(buildId) == null) {
            return false;
        }

        this.queueSave = true;
        return true;
    }

    public synchronized boolean renamePreset(int buildId, String name) {
        // Sanity check name
        if (name == null || name.length() > GameConstants.DORM_MAX_NAME_LENGTH) {
            return false;
        }

        var preset = this.presets.get(buildId);
        if (preset == null) {
            return false;
        }

        preset.setBuildName(name);
        this.queueSave = true;

        return true;
    }

    public synchronized boolean renameRoom(int roomId, String name) {
        // Sanity check name
        if (name == null || name.length() > GameConstants.DORM_MAX_NAME_LENGTH) {
            return false;
        }

        // Check rename cooldown
        long now = Nebula.getCurrentServerTime();

        if (roomId == 0) {
            if (now < this.dormNameRestTime + GameConstants.DORM_NAME_RESET_TIME_LIMIT) {
                return false;
            }

            this.dormName = name;
            this.dormNameRestTime = now;
            this.queueSave = true;

            return true;
        }

        if (!this.isRoomUnlocked(roomId)) {
            return false;
        }

        var room = this.getOrCreateRoom(roomId);
        if (room == null || now < room.getRestNameTime() + GameConstants.DORM_NAME_RESET_TIME_LIMIT) {
            return false;
        }

        room.setRoomName(name);
        room.setRestNameTime(now);
        this.queueSave = true;

        return true;
    }

    public synchronized boolean setRoomBgm(int roomId, int discId) {
        if (discId < 0 || !this.isRoomUnlocked(roomId)) {
            return false;
        }

        var room = this.getOrCreateRoom(roomId);
        if (room == null) {
            return false;
        }

        room.setDiscId(discId);
        this.queueSave = true;

        return true;
    }

    public synchronized boolean setRoomScene(int roomId, int sceneId) {
        if (sceneId < 0 || !this.isRoomUnlocked(roomId)) {
            return false;
        }

        var roomData = GameData.getDormRoomDataTable().get(roomId);

        if (sceneId != 0) {
            if (!GameData.getDormRoomSceneDataTable().containsKey(sceneId)) {
                return false;
            }
            
            // The default scene of the room is always available
            if (roomData == null || roomData.getSceneRes() != sceneId) {
                if (this.getPlayer().getInventory().getItemCount(sceneId) <= 0) {
                    return false;
                }
            }
        }

        var room = this.getOrCreateRoom(roomId);
        if (room == null) {
            return false;
        }

        room.setRoomScene(sceneId);
        this.queueSave = true;

        return true;
    }

    public synchronized boolean updateInvitationRoles(int roomId, List<Integer> roleIds) {
        if (!this.isRoomUnlocked(roomId) || roleIds == null) {
            return false;
        }

        var roomData = GameData.getDormRoomDataTable().get(roomId);
        var room = this.getOrCreateRoom(roomId);

        if (roomData == null || room == null) {
            return false;
        }

        // Validate role ids
        var requested = new IntOpenHashSet();
        for (int roleId : roleIds) {
            var roleData = GameData.getDormRoleDataTable().get(roleId);
            if (roleData == null) {
                return false;
            }

            // Playable characters need to be owned by the player.
            if (roleData.getRoleType() == 1 && this.getPlayer().getCharacters().getCharacterById(roleId) == null) {
                return false;
            }

            requested.add(roleId);
        }

        // Keep existing invitation NPCs, mirroring the client
        var newRoles = new ArrayList<DormRole>();
        for (var role : room.getRoleList()) {
            var roleData = GameData.getDormRoleDataTable().get(role.getRoleId());
            if (role.isInvitationRole() && roleData != null && roleData.getRoleType() == 2) {
                newRoles.add(role);
            }
        }

        // Add requested roles
        var existing = new IntOpenHashSet();
        for (var role : newRoles) {
            existing.add(role.getRoleId());
        }

        for (int roleId : requested) {
            if (existing.contains(roleId)) {
                continue;
            }

            var role = new DormRole(roleId);
            role.setVisit(true);
            role.setInvitationRole(false);
            newRoles.add(role);
        }

        if (newRoles.size() > roomData.getRoleNum()) {
            return false;
        }

        room.setRoleList(newRoles);

        // Remove the requested roles from all other rooms
        for (var other : this.rooms.values()) {
            if (other.getDormRoomId() == roomId) {
                continue;
            }

            other.getRoleList().removeIf(role -> requested.contains(role.getRoleId()));
        }
        this.queueSave = true;
        return true;
    }

    // Interactions

    public boolean hasInteracted(int roleId) {
        this.resetDailies();
        return this.dailyInteractionRoleIds.contains(roleId);
    }

    public int getDailyInteractionCount() {
        this.resetDailies();
        return this.dailyInteractionRoleIds.size();
    }

    public synchronized void addInteractionRole(int roleId) {
        if (this.dailyInteractionRoleIds.add(roleId)) {
            this.queueSave = true;
        }
    }

    public boolean isParticipatingInRoom(int roomId, int roleId) {
        var room = this.rooms.get(roomId);
        if (room == null) {
            return false;
        }

        for (var role : room.getRoleList()) {
            if (role.getRoleId() == roleId) {
                return true;
            }
        }

        return false;
    }

    public synchronized int interactRole(GameCharacter character, int roleId, PlayerChangeInfo change) {
        if (character == null) {
            return -1;
        }

        // Affinity
        character.addAffinityExp(GameConstants.DORM_ROLE_FAVORABILITY, change);

        // Random interaction reward
        int dropItemId = emu.nebula.data.resources.DropPkgDef.getRandomDrop(GameConstants.DORM_ROLE_INTERACTION_DROP_ID);
        if (dropItemId > 0) {
            this.getPlayer().getInventory().addItem(dropItemId, 1, change);
        }

        this.addInteractionRole(roleId);
        this.getPlayer().trigger(QuestCondition.DormRoleInteractionReward, 1);

        return dropItemId;
    }

    /**
     * Interacts with the roles of the given rooms.
     * 
     * @param interactions the requested role interactions
     * @return the result of the interactions, or null if no interaction was possible
     */
    public synchronized DormRoleInteractionResult interactRoles(Iterable<RoleInteraction> interactions) {
        var change = new PlayerChangeInfo();
        var results = new ArrayList<DormRoleInteractionResult.Interaction>();
        var processed = new IntOpenHashSet();

        for (var interaction : interactions) {
            int roomId = interaction.getDormRoomId();
            int roleId = interaction.getRoleId();

            // Sanity checks
            if (roleId <= 0 || !processed.add(roleId)) {
                continue;
            }
            if (!this.isRoomUnlocked(roomId) || !this.isParticipatingInRoom(roomId, roleId)) {
                continue;
            }
            if (this.getDailyInteractionCount() >= GameConstants.DORM_ROLE_INTERACTION_MAX_NUM) {
                break;
            }
            if (this.hasInteracted(roleId)) {
                continue;
            }

            var character = this.getPlayer().getCharacters().getCharacterById(roleId);
            if (character == null) {
                continue;
            }

            int dropItemId = this.interactRole(character, roleId, change);
            if (dropItemId < 0) {
                continue;
            }

            results.add(new DormRoleInteractionResult.Interaction(character, dropItemId));
        }

        if (results.isEmpty()) {
            return null;
        }

        return new DormRoleInteractionResult(change, results);
    }

    // Affinity gifts

    public int getDailyGiftTotal() {
        return this.dailyAffinityGiftTotal;
    }

    public int getDailyGiftCount(int roleId) {
        return this.dailyAffinityGiftCounts.get(roleId);
    }

    public synchronized void addGift(int roleId, int count) {
        this.dailyAffinityGiftCounts.put(roleId, this.dailyAffinityGiftCounts.get(roleId) + count);
        this.dailyAffinityGiftTotal += count;
        this.queueSave = true;
    }

    /**
     * Sends affinity gifts to a character. One gift action per character per day,
     * with a shared daily item limit.
     * 
     * @return the changes of the gift, or null if the gifts could not be sent
     */
    public synchronized PlayerChangeInfo sendAffinityGift(GameCharacter character, int roomId, ItemParamMap items) {
        // Validate room and gifts
        if (character == null || !this.isRoomUnlocked(roomId) || items == null || items.isEmpty()) {
            return null;
        }
        
        int charId = character.getCharId();
        
        // Validate gift items
        int giftCount = 0;

        for (var entry : items.int2IntEntrySet()) {
            if (entry.getIntValue() <= 0) {
                return null;
            }

            giftCount += entry.getIntValue();

            if (giftCount > GameConstants.DORM_ROLE_AFFINITY_GIFT_MAX_NUM) {
                return null;
            }
        }

        // One gift action per character per day, with a shared daily item limit
        if (this.getDailyGiftCount(charId) > 0) {
            return null;
        }
        if (this.getDailyGiftTotal() + giftCount > GameConstants.DORM_ROLE_AFFINITY_GIFT_MAX_NUM) {
            return null;
        }

        // Remove items and add affinity
        var change = character.sendGift(items);

        if (change == null) {
            return null;
        }

        this.addGift(charId, giftCount);
        this.getPlayer().trigger(QuestCondition.DormSendGift, giftCount);

        return change;
    }

    // Blocks

    /**
     * Converts dorm blocks into furniture coins.
     * 
     * @return the changes of the conversion, or null if the conversion failed
     */
    public synchronized PlayerChangeInfo convertBlocks(int count) {
        if (count <= 0 || count > Integer.MAX_VALUE / GameConstants.DORM_BLOCKS_CONVERT_PROP) {
            return null;
        }
        
        var inventory = this.getPlayer().getInventory();
        
        if (inventory.getItemCount(GameConstants.DORM_JADE_ITEM_ID) < count) {
            return null;
        }
        
        var change = new PlayerChangeInfo();
        
        // Remove jade and add furniture coins
        inventory.removeItem(GameConstants.DORM_JADE_ITEM_ID, count, change);
        inventory.addItem(
                GameConstants.DORM_FURNITURE_COIN_ITEM_ID,
                count * GameConstants.DORM_BLOCKS_CONVERT_PROP,
                change
        );
        
        return change;
    }

    // Rewards
    
    public IntSet getClaimableRewardIds() {
        var result = new IntOpenHashSet();
        var unlocked = this.getUnlockedRoomIds();

        for (var reward : GameData.getDormRewardDataTable()) {
            if (this.receivedRewardIds.contains(reward.getId())) {
                continue;
            }

            if (reward.isUnlocked(unlocked)) {
                result.add(reward.getId());
            }
        }

        return result;
    }

    public boolean isLayoutRewardClaimable(int layoutId) {
        var data = GameData.getDormLayoutRewardDataTable().get(layoutId);
        if (data == null || data.getRewardId1() <= 0 || data.getQty1() <= 0) {
            return false;
        }

        for (int i = 1; i <= data.getFurnitureCount(); i++) {
            var layout = GameData.getDormRoomLayoutDataTable().get(layoutId * 100 + i);
            if (layout == null) {
                continue;
            }

            int required = 0;
            for (int j = 1; j <= data.getFurnitureCount(); j++) {
                var other = GameData.getDormRoomLayoutDataTable().get(layoutId * 100 + j);
                if (other != null && other.getFurnitureId() == layout.getFurnitureId()) {
                    required++;
                }
            }

            if (this.getFurnitureCount(layout.getFurnitureId()) < required) {
                return false;
            }
        }

        return true;
    }

    public synchronized PlayerChangeInfo receiveLayoutRewards(int[] layoutIds) {
        var change = new PlayerChangeInfo();
        var rewards = new ItemParamMap();
        boolean received = false;

        for (int layoutId : layoutIds) {
            if (this.receivedLayoutRewardIds.contains(layoutId) || !this.isLayoutRewardClaimable(layoutId)) {
                continue;
            }

            var data = GameData.getDormLayoutRewardDataTable().get(layoutId);
            rewards.add(data.getRewardId1(), data.getQty1());
            this.receivedLayoutRewardIds.add(layoutId);
            received = true;
        }

        if (!received) {
            return null;
        }

        this.getPlayer().getInventory().addItems(rewards, change);
        this.getPlayer().trigger(QuestCondition.DormRewardReceive, 1);
        this.queueSave = true;

        return change;
    }

    public synchronized PlayerChangeInfo receiveRewards(int[] rewardIds) {
        var change = new PlayerChangeInfo();
        var unlocked = this.getUnlockedRoomIds();
        boolean received = false;

        for (int rewardId : rewardIds) {
            if (this.receivedRewardIds.contains(rewardId)) {
                continue;
            }

            var data = GameData.getDormRewardDataTable().get(rewardId);
            if (data == null || !data.isUnlocked(unlocked)) {
                continue;
            }

            int itemId = DropPkgDef.getRandomDrop(data.getDropId());
            if (itemId <= 0) {
                continue;
            }

            this.getPlayer().getInventory().addItem(itemId, 1, change);
            this.receivedRewardIds.add(rewardId);
            received = true;
        }

        if (!received) {
            return null;
        }

        this.getPlayer().trigger(QuestCondition.DormDailyReceiveReward, 1);
        this.queueSave = true;

        return change;
    }

    public int getCollectionValue() {
        if (this.furnitureCollection == null) {
            return 0;
        }

        int total = 0;

        for (var entry : this.furnitureCollection.int2IntEntrySet()) {
            if (entry.getIntValue() <= 0) {
                continue;
            }

            var furniture = GameData.getDormFurnitureDataTable().get(entry.getIntKey());
            if (furniture != null) {
                total += furniture.getCollectionValue();
            }
        }

        return total;
    }

    public synchronized AchievementComplete receiveAchievements(int[] achievementIds) {
        int collectionValue = this.getCollectionValue();
        var change = new PlayerChangeInfo();
        var proto = AchievementComplete.newInstance();
        var rewards = new ItemParamMap();
        boolean completed = false;

        for (int achievementId : achievementIds) {
            if (this.completedAchievements.containsKey(achievementId)) {
                continue;
            }

            var data = GameData.getDormAchievementDataTable().get(achievementId);
            if (data == null || data.getCompleteCond() != 1) {
                continue;
            }

            int required = data.getCompleteParams() != null && data.getCompleteParams().length > 0
                    ? data.getCompleteParams()[0]
                    : 0;

            if (collectionValue < required) {
                continue;
            }

            long completeTime = Nebula.getCurrentServerTime();
            this.completedAchievements.put(achievementId, completeTime);

            if (data.getTid1() > 0 && data.getQty1() > 0) {
                rewards.add(data.getTid1(), data.getQty1());
            }
            if (data.getTid2() > 0 && data.getQty2() > 0) {
                rewards.add(data.getTid2(), data.getQty2());
            }
            if (data.getTid3() > 0 && data.getQty3() > 0) {
                rewards.add(data.getTid3(), data.getQty3());
            }

            var info = AchievementInfo.newInstance()
                    .setAchievementId(achievementId)
                    .setCompleteTime(completeTime);

            proto.addAchievementList(info);
            completed = true;
        }

        if (!completed) {
            return null;
        }

        this.getPlayer().getInventory().addItems(rewards, change);
        proto.setChangeInfo(change.toProto());
        this.queueSave = true;

        return proto;
    }

    // Social

    public boolean hasLikedFriend(int uid) {
        return this.likedFriendUids.contains(uid);
    }

    public synchronized void addLikedFriend(int uid) {
        this.likedFriendUids.add(uid);
    }

    public synchronized void addLike() {
        this.likeNum++;
    }

    public synchronized void recordVisit(int visitorUid) {
        if (this.dailyVisitorUids.add(visitorUid)) {
            this.visitTotalNum++;
            this.dayVisitNum = this.dailyVisitorUids.size();
        }
    }

    // Helpers for friend dorm data

    public DormManager getFriendDorm(int uid) {
        var cached = Nebula.getGameContext().getPlayerModule().getPlayer(uid);

        if (cached == null) {
            return null;
        }
        
        if (cached.getDormManager() == null) {
            cached.loadManagerFromDatabase(DormManager.class);
        }
        
        return cached.getDormManager();
    }

    // Proto

    public synchronized DormInfoListResp toProto() {
        var proto = DormInfoListResp.newInstance();
        
        var unlocked = this.getUnlockedRoomIds();
        var sortedRooms = new ArrayList<>(unlocked);
        sortedRooms.sort(Integer::compareTo);

        for (int roomId : sortedRooms) {
            var room = this.getOrCreateRoom(roomId);
            if (room != null) {
                proto.addDormList(room.toProto());
            }
        }

        var info = DormInfoData.newInstance()
                .setDormName(this.dormName == null ? "" : this.dormName)
                .setLikeNum(this.likeNum)
                .setDayVisitNum(this.dayVisitNum)
                .setVisitTotalNum(this.visitTotalNum)
                .setRestNameTime(this.dormNameRestTime)
                .setTaskGroup(Public.UI32V.newInstance().setValue(0).setVersion(0));

        var rewardIds = new ArrayList<>(this.getClaimableRewardIds());
        rewardIds.sort(Integer::compareTo);
        for (int rewardId : rewardIds) {
            info.addRewardIds(rewardId);
        }

        for (var entry : this.completedAchievements.entrySet()) {
            info.addAchievementList(
                    AchievementInfo.newInstance()
                            .setAchievementId(entry.getKey())
                            .setCompleteTime(entry.getValue())
            );
        }

        proto.setDormInfo(info);

        for (int layoutId : this.receivedLayoutRewardIds) {
            proto.addReceivedRewardIds(layoutId);
        }

        for (int roleId : this.dailyInteractionRoleIds) {
            proto.addInteractionRoleIds(roleId);
        }

        for (var entry : this.dailyAffinityGiftCounts.int2IntEntrySet()) {
            proto.addAffinityGiftRoleInfos(
                    DormAffinityGiftInfo.newInstance()
                            .setRoleId(entry.getIntKey())
                            .setSendGiftCnt(entry.getIntValue())
            );
        }

        return proto;
    }

    public DormHot getDormHotProto() {
        return DormHot.newInstance()
            .setCollectionValue(this.getCollectionValue());
    }
    
    // Database

    @Override
    public void save() {
        Nebula.getGameDatabase().save(this);
        this.queueSave = false;
    }
}
