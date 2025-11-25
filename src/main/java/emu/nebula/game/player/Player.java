package emu.nebula.game.player;

import java.util.Stack;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Indexed;

import emu.nebula.GameConstants;
import emu.nebula.Nebula;
import emu.nebula.data.GameData;
import emu.nebula.database.GameDatabaseObject;
import emu.nebula.game.account.Account;
import emu.nebula.game.agent.AgentManager;
import emu.nebula.game.battlepass.BattlePassManager;
import emu.nebula.game.character.CharacterStorage;
import emu.nebula.game.formation.FormationManager;
import emu.nebula.game.friends.FriendList;
import emu.nebula.game.gacha.GachaManager;
import emu.nebula.game.infinitytower.InfinityTowerManager;
import emu.nebula.game.instance.InstanceManager;
import emu.nebula.game.inventory.Inventory;
import emu.nebula.game.mail.Mailbox;
import emu.nebula.game.quest.QuestCondType;
import emu.nebula.game.quest.QuestManager;
import emu.nebula.game.scoreboss.ScoreBossManager;
import emu.nebula.game.story.StoryManager;
import emu.nebula.game.tower.StarTowerManager;
import emu.nebula.game.vampire.VampireSurvivorManager;
import emu.nebula.net.GameSession;
import emu.nebula.net.NetMsgId;
import emu.nebula.net.NetMsgPacket;
import emu.nebula.proto.PlayerData.DictionaryEntry;
import emu.nebula.proto.PlayerData.DictionaryTab;
import emu.nebula.proto.PlayerData.PlayerInfo;
import emu.nebula.proto.Public.CharShow;
import emu.nebula.proto.Public.Energy;
import emu.nebula.proto.Public.Friend;
import emu.nebula.proto.Public.HonorInfo;
import emu.nebula.proto.Public.NewbieInfo;
import emu.nebula.proto.Public.QuestType;
import emu.nebula.proto.Public.Story;
import emu.nebula.proto.Public.WorldClass;
import emu.nebula.proto.Public.WorldClassRewardState;
import emu.nebula.proto.Public.Title;

import lombok.Getter;
import lombok.Setter;
import us.hebi.quickbuf.ProtoMessage;
import us.hebi.quickbuf.RepeatedInt;

@Getter
@Entity(value = "players", useDiscriminator = false)
public class Player implements GameDatabaseObject {
    @Id private int uid;
    @Indexed private String accountUid;
    
    private transient Account account;
    private transient GameSession session;
    
    // Details
    private String name;
    private String signature;
    private boolean gender;
    private int headIcon;
    private int skinId;
    private int titlePrefix;
    private int titleSuffix;
    private int[] honor;
    private int[] showChars;
    private int[] boards;
    
    private int level;
    private int exp;
    private int energy;
    private long energyLastUpdate;
   
    private long lastEpochDay;
    private long lastLogin;
    private long createTime;
    
    // Managers
    private final transient CharacterStorage characters;
    private final transient FriendList friendList;
    private final transient BattlePassManager battlePassManager;
    private final transient StarTowerManager starTowerManager;
    private final transient InstanceManager instanceManager;
    private final transient InfinityTowerManager infinityTowerManager;
    private final transient VampireSurvivorManager vampireSurvivorManager;
    private final transient ScoreBossManager scoreBossManager;
    @Indexed @Setter @Getter private String playerRemoteToken;
    
    // Referenced data
    private transient Inventory inventory;
    private transient FormationManager formations;
    private transient Mailbox mailbox;
    private transient GachaManager gachaManager;
    private transient PlayerProgress progress;
    private transient StoryManager storyManager;
    private transient QuestManager questManager;
    private transient AgentManager agentManager;
    
    // Extra
    private transient Stack<NetMsgPacket> nextPackages;
    private transient boolean loaded;
    
    @Deprecated // Morphia only
    public Player() {
        // Init player managers
        this.characters = new CharacterStorage(this);
        this.friendList = new FriendList(this);
        this.battlePassManager = new BattlePassManager(this);
        this.starTowerManager = new StarTowerManager(this);
        this.instanceManager = new InstanceManager(this);
        this.infinityTowerManager = new InfinityTowerManager(this);
        this.vampireSurvivorManager = new VampireSurvivorManager(this);
        this.scoreBossManager = new ScoreBossManager(this);
        
        // Init next packages stack
        this.nextPackages = new Stack<>();
    }
    
    public Player(Account account, String name, boolean gender) {
        this();
        
        // Set uid first
        if (account.getReservedPlayerUid() > 0) {
            this.uid = account.getReservedPlayerUid();
        } else {
            this.uid = Nebula.getGameDatabase().getNextObjectId(Player.class);
        }
        
        // Set basic info
        this.accountUid = account.getUid();
        this.createTime = Nebula.getCurrentTime();
        
        this.name = name;
        this.signature = "";
        this.gender = gender;
        this.headIcon = gender ? 101 : 102;
        this.skinId = 10301;
        this.titlePrefix = 1;
        this.titleSuffix = 2;
        this.honor = new int[3];
        this.showChars = new int[3];
        this.boards = new int[] {410301};
        this.playerRemoteToken = null;
        
        this.level = 1;
        this.energy = 240;
        this.energyLastUpdate = this.createTime;
        
        // Setup inventory
        this.inventory = new Inventory(this);
        
        // Add starter characters
        this.getCharacters().addCharacter(103);
        this.getCharacters().addCharacter(112);
        this.getCharacters().addCharacter(113);
        
        // Add starter discs
        this.getCharacters().addDisc(211001);
        this.getCharacters().addDisc(211005);
        this.getCharacters().addDisc(211007);
        this.getCharacters().addDisc(211008);
    }
    
    public Account getAccount() {
        if (this.account == null) {
            this.account = Nebula.getAccountDatabase().getObjectByField(Account.class, "_id", this.getAccountUid());
        }
        
        return this.account;
    }
    
    public void setSession(GameSession session) {
        if (this.session != null) {
            // Sanity check
            if (this.session == session) {
                return;
            }
            
            // Clear player from session
            this.session.clearPlayer();
        }
        
        // Set session
        this.session = session;
    }
    
    public void removeSession() {
        this.session = null;
        Nebula.getGameContext().getPlayerModule().removeFromCache(this);
    }
    
    public boolean hasSession() {
        return this.session != null;
    }
    
    public boolean getGender() {
        return this.gender;
    }

    public boolean editName(String newName) {
        // Sanity check
        if (newName == null || newName.isEmpty() || newName.equals(this.getName())) {
            return false;
        }
        
        // Limit name length
        if (newName.length() > 20) {
            newName = newName.substring(0, 19);
        }
        
        // Set name
        this.name = newName;
        
        // Update in database
        Nebula.getGameDatabase().update(this, this.getUid(), "name", this.getName());
        
        // Success
        return true;
    }
    
    public void editGender() {
        // Set name
        this.gender = !this.gender;
        
        // Update in database
        Nebula.getGameDatabase().update(this, this.getUid(), "gender", this.getGender());
    }

    public boolean editTitle(int prefix, int suffix) {
        // Check to make sure we own these titles
        if (!getInventory().getTitles().contains(prefix) || !getInventory().getTitles().contains(suffix)) {
            return false;
        }
        
        // Skip if we are not changing titles
        if (this.titlePrefix == prefix && this.titleSuffix == suffix) {
            return true;
        }
        
        // TODO check if title is prefix or suffix
        
        // Set
        this.titlePrefix = prefix;
        this.titleSuffix = suffix;
        
        // Update in database
        Nebula.getGameDatabase().update(this, this.getUid(), "titlePrefix", this.getTitlePrefix(), "titleSuffix", this.getTitleSuffix());
        
        return true;
    }
    
    public boolean editHeadIcon(int id) {
        // Skip if we are not changing head icon
        if (this.headIcon == id) {
            return true;
        }
        
        // Make sure we own the head icon
        if (!getInventory().hasHeadIcon(id)) {
            return false;
        }
        
        // Set
        this.headIcon = id;
        
        // Update in database
        Nebula.getGameDatabase().update(this, this.getUid(), "headIcon", this.getHeadIcon());
        
        // Success
        return true;
    }
    
    public boolean editSignature(String signature) {
        // Sanity check
        if (signature == null) {
            return false;
        }
        
        // Limit signature to 30 max chars
        if (signature.length() > 30) {
            signature = signature.substring(0, 29);
        }
        
        // Set signature
        this.signature = signature;
        
        // Update in database
        Nebula.getGameDatabase().update(this, this.getUid(), "signature", this.getSignature());
        
        // Success
        return true;
    }
    
    public boolean setSkin(int skinId) {
        // Skip if we are setting the same skin
        if (this.skinId == skinId) {
            return true;
        }
        
        // Make sure we own this skin
        if (!getInventory().hasSkin(skinId)) {
            return false;
        }
        
        // Set skin
        this.skinId = skinId;
        
        // Update in database
        Nebula.getGameDatabase().update(this, this.getUid(), "skinId", this.getSkinId());
        
        // Success
        return false;
    }

    public boolean setShowChars(RepeatedInt charIds) {
        // Sanity check
        if (charIds.length() > this.getShowChars().length) {
            return false;
        }
        
        // Verify that we have the correct characters
        for (int id : charIds) {
            if (id != 0 && !getCharacters().hasCharacter(id)) {
                return false;
            }
        }
        
        // TODO check duplicates
        
        // Clear
        this.showChars[0] = 0;
        this.showChars[1] = 0;
        this.showChars[2] = 0;
        
        // Set
        for (int i = 0; i < charIds.length(); i++) {
            this.showChars[i] = charIds.get(i);
        }
        
        // Update in database
        Nebula.getGameDatabase().update(this, this.getUid(), "showChars", this.getShowChars());
        
        // Success
        return true;
    }
    
    public boolean setHonor(RepeatedInt honorIds) {
        // Sanity check
        if (honorIds.length() > this.getHonor().length) {
            return false;
        }
        
        // Verify that we have the honor titles
        for (int id : honorIds) {
            if (id != 0 && !getInventory().getHonorList().contains(id)) {
                System.out.println(id);
                return false;
            }
        }
        
        // TODO check duplicates
        
        // Clear
        this.honor[0] = 0;
        this.honor[1] = 0;
        this.honor[2] = 0;
        
        // Set
        for (int i = 0; i < honorIds.length(); i++) {
            this.honor[i] = honorIds.get(i);
        }
        
        // Update in database
        Nebula.getGameDatabase().update(this, this.getUid(), "honor", this.getHonor());
        
        // Success
        return true;
    }

    public boolean setBoard(RepeatedInt ids) {
        // Length check
        if (ids.length() <= 0 || ids.length() > GameConstants.MAX_SHOWCASE_IDS) {
            return false;
        }
        
        // Get max length
        this.boards = new int[ids.length()];
        
        // Copy ids to our boards array
        for (int i = 0; i < ids.length(); i++) {
            int id = ids.get(i);
            this.boards[i] = id;
        }
        
        // Save to database
        Nebula.getGameDatabase().update(this, this.getUid(), "boards", this.getBoards());
        
        // Success
        return true;
    }
    
    public void setNewbieInfo(int groupId, int stepId) {
        // TODO 
    }
    
    public int getMaxExp() {
        var data = GameData.getWorldClassDataTable().get(this.level + 1);
        return data != null ? data.getExp() : 0;
    }
    
    public PlayerChangeInfo addExp(int amount, PlayerChangeInfo changes) {
        // Check if changes is null
        if (changes == null) {
            changes = new PlayerChangeInfo();
        }
        
        // Sanity
        if (amount <= 0) {
            return changes;
        }
        
        // Setup
        int oldLevel = this.getLevel();
        int oldExp = this.getExp();
        int expRequired = this.getMaxExp();

        // Add exp
        this.exp += amount;

        // Check for level ups
        while (this.exp >= expRequired && expRequired > 0) {
            // Add level
            this.level += 1;
            this.exp -= expRequired;
            
            // Recalculate exp required
            expRequired = this.getMaxExp();
            
            // Set level reward
            this.getQuestManager().getLevelRewards().setBit(this.level);
        }
        
        // Save to database
        Nebula.getGameDatabase().update(
                this, 
                this.getUid(), 
                "level", 
                this.getLevel(), 
                "exp", 
                this.getExp()
        );
        
        // Save level rewards if we changed it
        if (oldLevel != this.getLevel()) {
            this.getQuestManager().saveLevelRewards();
            
            this.addNextPackage(
                NetMsgId.world_class_reward_state_notify, 
                WorldClassRewardState.newInstance()
                    .setFlag(getQuestManager().getLevelRewards().toBigEndianByteArray())
            );
        }
        
        // Calculate changes
        var proto = WorldClass.newInstance()
                .setAddClass(this.getLevel() - oldLevel)
                .setExpChange(this.getExp() - oldExp);
        
        changes.add(proto);
        
        return changes;
    }
    
    // Energy
    
    public int getEnergy() {
        // Cache time
        long time = Nebula.getCurrentTime();
        
        // Calculate time diff
        double diff = time - this.energyLastUpdate;
        long bonusEnergy = (int) Math.floor(diff / GameConstants.ENERGY_REGEN_TIME);
        
        if (this.energy < GameConstants.MAX_ENERGY) {
            this.energy = Math.min(this.energy + (int) bonusEnergy, GameConstants.MAX_ENERGY);
            this.energyLastUpdate = (bonusEnergy * GameConstants.ENERGY_REGEN_TIME) + this.energyLastUpdate;
        } else {
            this.energyLastUpdate = time;
        }
        
        return this.energy;
    }
    
    public PlayerChangeInfo addEnergy(int amount, PlayerChangeInfo change) {
        // Sanity check
        if (amount <= 0) {
            return change == null ? new PlayerChangeInfo() : change;
        }
        
        // Complete
        return modifyEnergy(amount, change);
    }
    
    public PlayerChangeInfo consumeEnergy(int amount, PlayerChangeInfo change) {
        // Sanity check
        if (amount <= 0) {
            return change == null ? new PlayerChangeInfo() : change;
        }
        
        // Consume energy
        change = modifyEnergy(-amount, change);
        
        // Trigger quest
        this.triggerQuest(QuestCondType.EnergyDeplete, amount);
        
        // Complete
        return change;
    }
    
    private PlayerChangeInfo modifyEnergy(int amount, PlayerChangeInfo change) {
        // Check if changes is null
        if (change == null) {
            change = new PlayerChangeInfo();
        }
        
        // Update energy
        this.getEnergy();
        
        // Remove energy
        this.energy = Math.max(this.energy + amount, 0);
        
        // Save to database
        Nebula.getGameDatabase().update(
                this, 
                this.getUid(), 
                "energy", 
                this.getEnergy(), 
                "energyLastUpdate", 
                this.getEnergyLastUpdate()
        );
        
        // Add to change
        change.add(this.getEnergyProto());
        
        // Complete
        return change;
    }
    
    //
    
    public void sendMessage(String string) {
        // Empty
    }
    
    // Dailies
    
    public void checkResetDailies() {
        // Sanity check to make sure daily reset isnt being triggered wrong
        if (Nebula.getGameContext().getEpochDays() <= this.getLastEpochDay()) {
            return;
        }
        
        // Reset dailies
        this.resetDailies(false);
        
        // Update last epoch day
        this.lastEpochDay = Nebula.getGameContext().getEpochDays();
        Nebula.getGameDatabase().update(this, this.getUid(), "lastEpochDay", this.lastEpochDay);
    }

    public void resetDailies(boolean resetWeekly) {
        // Reset daily quests
        this.getQuestManager().resetDailyQuests();
        this.getBattlePassManager().getBattlePass().resetDailyQuests(resetWeekly);
    }
    
    // Trigger quests
    
    public void triggerQuest(QuestCondType condition, int progress) {
        this.triggerQuest(condition, progress, 0);
    }
    
    public void triggerQuest(QuestCondType condition, int progress, int param) {
        this.getQuestManager().trigger(condition, progress, param);
        this.getBattlePassManager().getBattlePass().trigger(condition, progress, param);
    }
    
    // Login
    
    private <T extends PlayerManager> T loadManagerFromDatabase(Class<T> cls) {
        var manager = Nebula.getGameDatabase().getObjectByField(cls, "_id", this.getUid());
        
        if (manager != null) {
            manager.setPlayer(this);
        } else {
            try {
                manager = cls.getDeclaredConstructor(Player.class).newInstance(this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        return manager;
    }
    
    /**
     * Called when the player is loaded from the database
     */
    public void onLoad() {
        // Load from database
        this.getCharacters().loadFromDatabase();
        this.getFriendList().loadFromDatabase();
        this.getStarTowerManager().loadFromDatabase();
        this.getBattlePassManager().loadFromDatabase();
        
        // Load inventory before referenced classes
        if (this.inventory == null) {
            this.inventory = this.loadManagerFromDatabase(Inventory.class);
        }
        this.getInventory().loadFromDatabase();
        
        // Load referenced classes from the database
        this.formations = this.loadManagerFromDatabase(FormationManager.class);
        this.mailbox = this.loadManagerFromDatabase(Mailbox.class);
        this.progress = this.loadManagerFromDatabase(PlayerProgress.class);
        this.gachaManager = this.loadManagerFromDatabase(GachaManager.class);
        this.storyManager = this.loadManagerFromDatabase(StoryManager.class);
        this.questManager = this.loadManagerFromDatabase(QuestManager.class);
        this.agentManager = this.loadManagerFromDatabase(AgentManager.class);
        
        // Database fixes
        if (this.showChars == null) {
            this.showChars = new int[3];
            this.save();
        }
        
        // Load complete
        this.loaded = true;
    }
    
    public void onLogin() {
        // See if we need to reset dailies
        this.checkResetDailies();
        
        // Trigger quest login
        this.triggerQuest(QuestCondType.LoginTotal, 1);
        
        // Update last login time
        this.lastLogin = System.currentTimeMillis();
        Nebula.getGameDatabase().update(this, this.getUid(), "lastLogin", this.getLastLogin());
    }
    
    // Next packages
    
    public boolean hasNextPackages() {
        return this.getNextPackages().size() > 0;
    }
    
    public void addNextPackage(int msgId, ProtoMessage<?> proto) {
        this.getNextPackages().add(new NetMsgPacket(msgId, proto));
    }
    
    // Proto

    public PlayerInfo toProto() {
        PlayerInfo proto = PlayerInfo.newInstance()
                .setServerTs(Nebula.getCurrentTime())
                .setDailyShopRewardStatus(this.getQuestManager().hasDailyReward())
                .setAchievements(new byte[64]);
        
        var acc = proto.getMutableAcc()
            .setNickName(this.getName())
            .setSignature(this.getSignature())
            .setGender(this.getGender())
            .setId(this.getUid())
            .setHeadIcon(this.getHeadIcon())
            .setSkinId(this.getSkinId())
            .setTitlePrefix(this.getTitlePrefix())
            .setTitleSuffix(this.getTitleSuffix())
            .setCreateTime(this.getCreateTime());
        
        // Set showcase character
        for (int charId : this.getShowChars()) {
            var info = CharShow.newInstance();
            var character = this.getCharacters().getCharacterById(charId);
            
            if (character != null) {
                info.setCharId(character.getCharId())
                    .setLevel(character.getLevel())
                    .setSkin(character.getSkin());
            }
            
            acc.addChars(info);
        }
        
        // Set honor
        for (int honorId : this.getHonor()) {
            var info = HonorInfo.newInstance();
            
            if (honorId != 0) {
                info.setId(honorId);
            }
            
            proto.addHonors(info);
        }
        
        this.getInventory().getHonorList().forEach(proto::addHonorList);
        
        // Set world class
        proto.getMutableWorldClass()
            .setCur(this.getLevel())
            .setLastExp(this.getExp());
        
        proto.getMutableEnergy().setEnergy(this.getEnergyProto());
        
        // Add characters/discs/res/items
        for (var character : getCharacters().getCharacterCollection()) {
            proto.addChars(character.toProto());
        }
        
        for (var disc : getCharacters().getDiscCollection()) {
            proto.addDiscs(disc.toProto());
        }
        
        for (var item : getInventory().getItems().values()) {
            proto.addItems(item.toProto());
        }
        
        for (var res : getInventory().getResources().values()) {
            proto.addRes(res.toProto());
        }
        
        // Formations
        var formations = proto.getMutableFormation();
        for (var f : this.getFormations().getFormations().values()) {
            formations.addInfo(f.toProto());
        }
        
        // Set player states
        var state = proto.getMutableState()
            .setStorySet(true)
            .setFriend(this.getFriendList().hasPendingRequests());
        
        state.getMutableMail()
            .setNew(this.getMailbox().hasNewMail());
        
        state.getMutableBattlePass()
            .setState(1);
        
        state.getMutableFriendEnergy();
        state.getMutableMallPackage();
        state.getMutableAchievement();
        state.getMutableTravelerDuelQuest()
            .setType(QuestType.TravelerDuel);
        state.getMutableTravelerDuelChallengeQuest()
            .setType(QuestType.TravelerDuelChallenge);
        state.getMutableStarTower();
        state.getMutableStarTowerBook();
        state.getMutableScoreBoss();
        state.getMutableCharAffinityRewards();
        
        // Force complete tutorials
        for (var guide : GameData.getGuideGroupDataTable()) {
            var info = NewbieInfo.newInstance()
                    .setGroupId(guide.getId())
                    .setStepId(-1);
            
            acc.addNewbies(info);
        }
        
        acc.addNewbies(NewbieInfo.newInstance().setGroupId(GameConstants.INTRO_GUIDE_ID).setStepId(-1));
        
        // Story
        var story = proto.getMutableStory();
        
        for (int storyId : this.getStoryManager().getCompletedStories()) {
            var storyProto = Story.newInstance()
                    .setIdx(storyId);
            
            story.addStories(storyProto);
        }
        
        // Add titles
        for (int titleId : this.getInventory().getTitles()) {
            var titleProto = Title.newInstance()
                    .setTitleId(titleId);
            
            proto.addTitles(titleProto);
        }
        
        // Add board ids
        for (int boardId : this.getBoards()) {
            proto.addBoard(boardId);
        }
        
        // Quests
        this.getQuestManager().encodeProto(proto);
        
        // Add dictionary tabs
        for (var dictionaryData : GameData.getDictionaryTabDataTable()) {
            var dictionaryProto = DictionaryTab.newInstance()
                    .setTabId(dictionaryData.getId());
            
            for (var entry : dictionaryData.getEntries()) {
                var entryProto = DictionaryEntry.newInstance()
                        .setIndex(entry.getIndex())
                        .setStatus(2); // 2 = complete
                
                dictionaryProto.addEntries(entryProto);
            }
            
            proto.addDictionaries(dictionaryProto);
        }
        
        // Add progress
        this.getProgress().encodeProto(proto);
        
        // Handbook
        proto.addHandbook(this.getCharacters().getCharacterHandbook());
        proto.addHandbook(this.getCharacters().getDiscHandbook());
        
        // Phone
        var phone = proto.getMutablePhone();
        phone.setNewMessage(this.getCharacters().getNewPhoneMessageCount());
        
        // Agent
        var agentProto = proto.getMutableAgent();
        
        for (var agent : getAgentManager().getAgents().values()) {
            agentProto.addInfos(agent.toProto());
        }
        
        // Complete
        return proto;
    }
    
    public Friend getFriendProto() {
        var proto = Friend.newInstance()
                .setId(this.getUid())
                .setWorldClass(this.getLevel())
                .setHeadIcon(this.getHeadIcon())
                .setNickName(this.getName())
                .setSignature(this.getSignature())
                .setTitlePrefix(this.getTitlePrefix())
                .setTitleSuffix(this.getTitleSuffix())
                .setLastLoginTime(this.getLastLogin() * 1_000_000L);
        
        for (int charId : this.getShowChars()) {
            var info = CharShow.newInstance()
                    .setCharId(charId)
                    .setLevel(1)                    // TODO
                    .setSkin((charId * 100) + 1);   // TODO
            
            proto.addCharShows(info);
        }
        
        for (int honorId : this.getHonor()) {
            var info = HonorInfo.newInstance()
                    .setId(honorId);
            
            proto.addHonors(info);
        }
        
        return proto;
    }
    
    public Energy getEnergyProto() {
        long nextDuration = Math.max(GameConstants.ENERGY_REGEN_TIME - (Nebula.getCurrentTime() - getEnergyLastUpdate()), 1);
        
        var proto = Energy.newInstance()
                .setUpdateTime(this.getEnergyLastUpdate())
                .setNextDuration(nextDuration)
                .setPrimary(this.getEnergy())
                .setIsPrimary(true);
        
        return proto;
    }
    
}
