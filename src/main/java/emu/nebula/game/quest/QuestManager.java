package emu.nebula.game.quest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import emu.nebula.GameConstants;
import emu.nebula.Nebula;
import emu.nebula.data.GameData;
import emu.nebula.data.resources.StarTowerBookEventRewardDef;
import emu.nebula.data.resources.WorldClassDef;
import emu.nebula.database.GameDatabaseObject;
import emu.nebula.game.inventory.ItemParamMap;
import emu.nebula.game.player.Player;
import emu.nebula.game.player.PlayerChangeInfo;
import emu.nebula.game.player.PlayerManager;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.PlayerData.PlayerInfo;
import emu.nebula.util.Bitset;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

import lombok.AccessLevel;
import lombok.Getter;

@Getter
@Entity(value = "quests", useDiscriminator = false)
public class QuestManager extends PlayerManager implements GameDatabaseObject {
    @Id
    private int uid;
    
    // Daily activity missions
    private IntSet claimedActiveIds;
    private IntSet claimedWeeklyIds;
    
    // Quests
    private Map<Long, GameQuest> list;
    
    // Level rewards
    private Bitset levelRewards;
    
    // Claimed daily rewards
    @Getter(AccessLevel.NONE) private boolean dailyShopReward;
    @Getter(AccessLevel.NONE) private boolean dailyMallReward;
    
    @Deprecated // Morphia only
    public QuestManager() {
        
    }
    
    public QuestManager(Player player) {
        super(player);
        this.uid = player.getUid();
        this.claimedActiveIds = new IntOpenHashSet();
        this.claimedWeeklyIds = new IntOpenHashSet();
        this.list = new HashMap<>();
        this.levelRewards = new Bitset();
        
        this.initStarTowerQuests();
        this.initStarTowerBookEventQuests();
        
        this.resetDailyQuests(true);
        
        this.save();
    }
    
    public synchronized GameQuest getQuest(int type, int questId) {
        long key = ((long) type << 32) + questId;
        return this.list.get(key);
    }
    
    public synchronized GameQuest addQuest(GameQuest quest) {
        // Add to quest list
        this.list.put(quest.getKey(), quest);
        
        // Star Tower growth-node quests can complete retroactively if the player
        // already unlocked the required node(s) before the quest was added
        if (quest.getType() == QuestType.Tower) {
            var data = GameData.getStarTowerQuestDataTable().get(quest.getId());
            int cond = data != null ? data.getCompleteCond() : 0;
            
            if (cond == QuestCondition.TowerGrowthSpecificNote.getValue() || cond == QuestCondition.TowerGrowthWithSpecificNote.getValue()) {
                var manager = this.getPlayer().getStarTowerManager();
                int progress = 0;
                
                if (data.getParam1() != 0 && manager.hasGrowthNode(data.getParam1())) {
                    progress++;
                }
                
                if (data.getParam2() != 0 && manager.hasGrowthNode(data.getParam2())) {
                    progress++;
                }
                
                if (progress > 0) {
                    quest.setProgress(progress);
                }
            } else if (cond == QuestCondition.TowerBookWithSpecificEvent.getValue()) {
                this.syncStarTowerBookEventCountQuest(quest);
            }
        }
        
        // Return quest
        return quest;
    }
    
    public synchronized Collection<GameQuest> getQuests() {
        return this.getList().values();
    }
    
    public boolean hasDailyShopReward() {
        return !this.dailyShopReward;
    }
    
    public boolean hasDailyMallReward() {
        return !this.dailyMallReward;
    }
    
    public void saveLevelRewards() {
        Nebula.getGameDatabase().update(this, this.getUid(), "levelRewards", this.levelRewards);
    }
    
    public synchronized void resetDailyQuests(boolean resetWeekly) {
        // Reset daily quests
        for (var data : GameData.getDailyQuestDataTable()) {
            // Get quest
            var quest = getList().get(data.getQuestKey());
            
            if (quest == null) {
                quest = this.addQuest(new GameQuest(data));
            }
            
            // Reset progress
            quest.resetProgress();
            
            // Sync quest with player client
            this.syncQuest(quest);
        }
        
        // Reset weekly quests
        if (resetWeekly) {
            for (var data : GameData.getWeeklyQuestDataTable()) {
                // Get quest
                var quest = getList().get(data.getQuestKey());
                
                if (quest == null) {
                    quest = this.addQuest(new GameQuest(data));
                }
                
                // Reset progress
                quest.resetProgress();
                
                // Sync quest with player client
                this.syncQuest(quest);
            }
            
            // Reset weekly ids
            this.claimedWeeklyIds.clear();
        }
        
        // Reset activity
        this.claimedActiveIds.clear();
        
        // Reset daily shop rewards
        this.dailyShopReward = false;
        this.dailyMallReward = false;
        
        // Persist to database
        this.save();
    }

    public synchronized void trigger(int condition, int progress, int param1, int param2) {
        // TODO optimize so we dont loop through every quest
        for (var entry : getList().entrySet()) {
            // Get quest from entry
            var quest = entry.getValue();
            
            // Skip completed quests
            if (quest.isComplete()) {
                continue;
            }
            
            // Star Tower quests use their own generated params and matching rules
            // TODO clean this code up to look nice
            if (quest.getType() == QuestType.Tower) {
                // Get quest data
                var data = GameData.getStarTowerQuestDataTable().get(quest.getId());
                if (data == null || data.getCompleteCond() != condition) {
                    continue;
                }
                
                // Check params
                if (!StarTowerQuestHelper.matches(data, param1, param2)) {
                    continue;
                }
                
                // Add progress
                if (!quest.addProgress(progress)) {
                    continue;
                }
            } else {
                // Now we try to trigger regular daily/weekly quests
                boolean result = quest.trigger(condition, progress, param1, param2);
                
                // Skip if quest progress wasn't changed
                if (!result) {
                    continue;
                }
            }
            
            // Sync quest with player client
            this.syncQuest(quest);
            
            // Update in database
            Nebula.getGameDatabase().update(this, this.getUid(), "list." + entry.getKey(), quest);
        }
    }
    
    /**
     * Update this quest on the player client
     */
    public void syncQuest(GameQuest quest) {
        if (!getPlayer().hasSession()) {
            return;
        }
        
        getPlayer().addNextPackage(
            NetMsgId.quest_change_notify, 
            quest.toProto()
        );
    }
    
    // Star Tower quests
    
    /**
     * Adds all currently unlocked Star Tower quests.
     * Returns true if any quest was added or refreshed.
     */
    public synchronized boolean initStarTowerQuests() {
        boolean changed = false;
        
        for (var data : GameData.getStarTowerQuestDataTable()) {
            // Only add quests whose prerequisites have been claimed
            if (!StarTowerQuestHelper.isUnlocked(this, data)) {
                continue;
            }
            
            // Get existing quest
            var quest = this.getList().get(data.getQuestKey());
            
            if (quest == null) {
                quest = this.addQuest(new GameQuest(data));
                changed = true;
            } else if (quest.refreshData(data)) {
                changed = true;
            }
            
            // Book collection quests can complete retroactively
            if (this.syncStarTowerBookEventCountQuest(quest)) {
                changed = true;
            }
        }
        
        return changed;
    }
    
    /**
     * Adds quests that became available after a prerequisite was claimed.
     */
    public synchronized void unlockStarTowerQuests() {
        boolean changed = false;
        
        for (var data : GameData.getStarTowerQuestDataTable()) {
            // Skip quests that already exist
            if (this.getList().containsKey(data.getQuestKey())) {
                continue;
            }
            
            // Only add quests whose prerequisites have been claimed
            if (!StarTowerQuestHelper.isUnlocked(this, data)) {
                continue;
            }
            
            // Add quest and sync it with the client
            var quest = this.addQuest(new GameQuest(data));
            
            // Syncs quest with client
            this.syncQuest(quest);
            
            // Set changed flag
            changed = true;
        }
        
        // Persist new quests
        if (changed) {
            this.save();
        }
    }
    
    // Star Tower book events (anecdotes)
    
    /**
     * Returns the total number of anecdotes the player has collected.
     */
    public synchronized int getClaimedStarTowerBookEventCount() {
        int count = 0;
        
        for (var quest : this.getQuests()) {
            if (quest.getType() == QuestType.TowerEvent && quest.isClaimed()) {
                count++;
            }
        }
        
        return count;
    }
    
    /**
     * Syncs the progress of Star Tower book collection quests.
     */
    private boolean syncStarTowerBookEventCountQuest(GameQuest quest) {
        if (quest.getCond() != QuestCondition.TowerBookWithSpecificEvent.getValue()) {
            return false;
        }
        
        int progress = Math.min(this.getClaimedStarTowerBookEventCount(), quest.getMaxProgress());
        
        if (progress <= quest.getCurProgress()) {
            return false;
        }
        
        return quest.setProgress(progress);
    }
    
    /**
     * Adds all Star Tower book event rewards as quests.
     * Returns true if any quest was added or refreshed.
     */
    public synchronized boolean initStarTowerBookEventQuests() {
        boolean changed = false;
        
        for (var data : GameData.getStarTowerBookEventRewardDataTable()) {
            // Get existing quest
            var quest = this.getList().get(data.getQuestKey());
            
            if (quest == null) {
                quest = this.addQuest(new GameQuest(data));
                changed = true;
            } else if (quest.refreshData(data)) {
                changed = true;
            }
            
            // NPC affinity quests can complete retroactively
            if (this.syncStarTowerBookEventQuest(quest, data)) {
                changed = true;
            }
        }
        
        return changed;
    }
    
    /**
     * Syncs progress for book event quests that can be completed retroactively.
     */
    private boolean syncStarTowerBookEventQuest(GameQuest quest, StarTowerBookEventRewardDef data) {
        if (data.getCompleteCond() != QuestCondition.NpcAffinityWithSpecificLevel.getValue()) {
            return false;
        }
        
        var npc = this.getPlayer().getProgress().getTowerNpcs().get(quest.getParam());
        
        if (npc == null) {
            return false;
        }
        
        int progress = Math.min(npc.getLevel(), quest.getMaxProgress());
        
        if (progress <= quest.getCurProgress()) {
            return false;
        }
        
        return quest.setProgress(progress);
    }
    
    public synchronized PlayerChangeInfo receiveStarTowerBookEventReward(int questId) {
        // Get received quests
        var claimList = new ArrayList<GameQuest>();
        
        if (questId > 0) {
            // Claim specific quest
            var quest = this.getQuest(QuestType.TowerEvent, questId);
            
            if (quest != null && quest.canClaim()) {
                claimList.add(quest);
            }
        } else {
            // Claim all
            for (var quest : this.getQuests()) {
                if (quest.getType() != QuestType.TowerEvent) {
                    continue;
                }
                
                if (!quest.canClaim()) {
                    continue;
                }
                
                claimList.add(quest);
            }
        }
        
        // Sanity check
        if (claimList.isEmpty()) {
            return null;
        }
        
        // Create change info
        var change = new PlayerChangeInfo();
        var received = new IntArrayList();
        
        // Claim
        for (var quest : claimList) {
            // Get data
            var data = GameData.getStarTowerBookEventRewardDataTable().get(quest.getId());
            
            if (data != null) {
                // Add reward
                this.getPlayer().getInventory().addItem(data.getItemId(), data.getItemQty(), change);
            }
            
            // Set claimed
            quest.setClaimed(true);
            
            // Add received id
            received.add(quest.getId());
            
            // Update in database
            this.saveQuest(quest);
        }
        
        // Update Star Tower book collection quests
        this.trigger(QuestCondition.TowerBookWithSpecificEvent.getValue(), claimList.size(), 0, 0);
        
        // Set received ids as extra data
        change.setExtraData(received);
        
        // Success
        return change.setSuccess(true);
    }
    
    public synchronized PlayerChangeInfo receiveStarTowerQuestReward(int questId) {
        // Get received quests
        var claimList = new ArrayList<GameQuest>();
        
        if (questId > 0) {
            // Claim specific quest
            var quest = this.getQuest(QuestType.Tower, questId);
            
            if (quest != null && quest.canClaim()) {
                claimList.add(quest);
            }
        } else {
            // Claim all
            for (var quest : this.getQuests()) {
                if (quest.getType() != QuestType.Tower) {
                    continue;
                }
                
                if (!quest.canClaim()) {
                    continue;
                }
                
                claimList.add(quest);
            }
        }
        
        // Sanity check
        if (claimList.isEmpty()) {
            return null;
        }
        
        // Create change info
        var change = new PlayerChangeInfo();
        
        // Claim
        for (var quest : claimList) {
            // Get data
            var data = GameData.getStarTowerQuestDataTable().get(quest.getId());
            
            if (data != null) {
                // Add rewards
                this.getPlayer().getInventory().addItems(data.getRewards(), change);
            }
            
            // Set claimed
            quest.setClaimed(true);
            
            // Update in database
            this.saveQuest(quest);
        }
        
        // Unlock newly available quests
        this.unlockStarTowerQuests();
        
        // Success
        return change.setSuccess(true);
    }
    
    // Daily quests
    
    public synchronized int getDailyActivity() {
        int activity = 0;
        
        for (var quest : this.getQuests()) {
            if (quest.getType() != QuestType.Daily) {
                continue;
            }
            
            if (!quest.isClaimed()) {
                continue;
            }
            
            var data = GameData.getDailyQuestDataTable().get(quest.getId());
            if (data == null) continue;
            
            activity += data.getActive();
        }
        
        return activity;
    }
    
    public PlayerChangeInfo receiveDailyQuestReward(int questId) {
        // Get received quests
        var claimList = new ArrayList<GameQuest>();
        
        if (questId > 0) {
            // Claim specific quest
            var quest = this.getQuest(QuestType.Daily, questId);
            
            if (quest != null && !quest.isClaimed()) {
                claimList.add(quest);
            }
        } else {
            // Claim all
            for (var quest : this.getQuests()) {
                if (quest.getType() != QuestType.Daily) {
                    continue;
                }
                
                if (!quest.canClaim()) {
                    continue;
                }
                
                claimList.add(quest);
            }
        }
        
        // Sanity check
        if (claimList.isEmpty()) {
            return null;
        }

        // Create change info
        var change = new PlayerChangeInfo();
        
        // Claim
        for (var quest : claimList) {
            // Get data
            var data = GameData.getDailyQuestDataTable().get(quest.getId());
            if (data != null) {
                // Add reward data
                this.getPlayer().getInventory().addItem(data.getItemTid(), data.getItemQty(), change);
            }
            
            // Set claimed
            quest.setClaimed(true);
            
            // Update in database
            this.saveQuest(quest);
        }
        
        // Trigger quest
        this.getPlayer().trigger(QuestCondition.QuestWithSpecificType, claimList.size(), QuestType.Daily);
        
        // Success
        return change.setSuccess(true);
    }
    
    public PlayerChangeInfo claimDailyActiveRewards() {
        // Init
        var claimList = new IntArrayList();
        var rewards = new ItemParamMap();
        
        int activity = this.getDailyActivity();
        
        // Get claimable 
        for (var data : GameData.getDailyQuestActiveDataTable()) {
            if (this.getClaimedActiveIds().contains(data.getId())) {
                continue;
            }
            
            if (activity >= data.getActive()) {
                // Add rewards
                rewards.add(data.getRewards());
                
                // Add to claimed activity list
                claimList.add(data.getId());
            }
        }
        
        // Sanity check
        if (claimList.isEmpty()) {
            return null;
        }
        
        // Add rewards
        var change = this.getPlayer().getInventory().addItems(rewards);
        
        // Set claimed list
        change.setExtraData(claimList);
        
        // Update in database
        this.getClaimedActiveIds().addAll(claimList);
        Nebula.getGameDatabase().update(this, this.getUid(), "claimedActiveIds", this.getClaimedActiveIds());
        
        // Success
        return change.setSuccess(true);
    }
    
    // Weekly quests
    
    public synchronized int getWeeklyActivity() {
        int activity = 0;
        
        for (var quest : getQuests()) {
            if (quest.getType() != QuestType.Weekly) {
                continue;
            }
            
            if (!quest.isClaimed()) {
                continue;
            }
            
            var data = GameData.getWeeklyQuestDataTable().get(quest.getId());
            if (data == null) continue;
            
            activity += data.getActive();
        }
        
        return activity;
    }
    
    public PlayerChangeInfo receiveWeeklyQuestReward(int questId) {
        // Get received quests
        var claimList = new ArrayList<GameQuest>();
        
        if (questId > 0) {
            // Claim specific quest
            var quest = this.getQuest(QuestType.Weekly, questId);
            
            if (quest != null && !quest.isClaimed()) {
                claimList.add(quest);
            }
        } else {
            // Claim all
            for (var quest : this.getQuests()) {
                if (quest.getType() != QuestType.Weekly) {
                    continue;
                }
                
                if (!quest.canClaim()) {
                    continue;
                }
                
                claimList.add(quest);
            }
        }
        
        // Sanity check
        if (claimList.isEmpty()) {
            return null;
        }

        // Create change info
        var change = new PlayerChangeInfo();
        
        // Claim
        for (var quest : claimList) {
            // Get data
            var data = GameData.getWeeklyQuestDataTable().get(quest.getId());
            if (data != null) {
                // Add reward data
                this.getPlayer().getInventory().addItem(data.getItemTid(), data.getItemQty(), change);
            }
            
            // Set claimed
            quest.setClaimed(true);
            
            // Update in database
            this.saveQuest(quest);
        }
        
        // Trigger quest
        this.getPlayer().trigger(QuestCondition.QuestWithSpecificType, claimList.size(), QuestType.Weekly);
        
        // Success
        return change.setSuccess(true);
    }
    
    public PlayerChangeInfo claimWeeklyActiveRewards() {
        // Init
        var claimList = new IntArrayList();
        var rewards = new ItemParamMap();
        
        int activity = this.getWeeklyActivity();
        
        // Get claimable 
        for (var data : GameData.getWeeklyQuestActiveDataTable()) {
            if (this.getClaimedWeeklyIds().contains(data.getId())) {
                continue;
            }
            
            if (activity >= data.getActive()) {
                // Add rewards
                rewards.add(data.getRewards());
                
                // Add to claimed activity list
                claimList.add(data.getId());
            }
        }
        
        // Sanity check
        if (claimList.isEmpty()) {
            return null;
        }
        
        // Add rewards
        var change = this.getPlayer().getInventory().addItems(rewards);
        
        // Set claimed list
        change.setExtraData(claimList);
        
        // Update in database
        this.getClaimedWeeklyIds().addAll(claimList);
        Nebula.getGameDatabase().update(this, this.getUid(), "claimedWeeklyIds", this.getClaimedWeeklyIds());
        
        // Success
        return change.setSuccess(true);
    }
    
    // World level rewards
    
    public PlayerChangeInfo receiveWorldClassReward(int id) {
        // Get rewards we want to claim
        var claimList = new ArrayList<WorldClassDef>();
        
        if (id > 0) {
            // Claim specific level reward
            if (this.getLevelRewards().isSet(id)) {
                var data = GameData.getWorldClassDataTable().get(id);
                if (data != null) {
                    claimList.add(data);
                }
            }
        } else {
            // Claim all
            for (var data : GameData.getWorldClassDataTable()) {
                if (this.getLevelRewards().isSet(data.getId())) {
                    claimList.add(data);
                }
            }
        }
        
        // Sanity check
        if (claimList.isEmpty()) {
            return null;
        }
        
        // Claim
        var rewards = new ItemParamMap();
        
        for (var data : claimList) {
            // Add rewards
            rewards.add(data.getRewards());
            
            // Unset level rewards
            this.getLevelRewards().unsetBit(data.getId());
        }
        
        // Add to inventory
        var change = this.getPlayer().getInventory().addItems(rewards);
        
        // Save to db
        this.saveLevelRewards();
        
        // Success
        return change.setSuccess(true);
    }
    
    // Daily rewards
    
    public PlayerChangeInfo claimDailyShopGift() {
        // Sanity check
        if (!this.hasDailyShopReward()) {
            return null;
        }
        
        // Daily shop reward
        var reward = GameConstants.DAILY_SHOP_GIFTS.next();
        var change = this.getPlayer().getInventory().addItem(reward.getId(), reward.getCount());
        
        // Set and update in database
        this.dailyShopReward = true;
        Nebula.getGameDatabase().update(this, this.getUid(), "dailyShopReward", this.dailyShopReward);
        
        // Trigger quest
        this.getPlayer().trigger(QuestCondition.DailyShopReceiveShopTotal, 1);
        
        // Success
        return change.setSuccess(true);
    }
    
    public PlayerChangeInfo claimDailyMallGift() {
        // Sanity check
        if (!this.hasDailyMallReward()) {
            return null;
        }
        
        // Daily mall reward
        var rewards = GameConstants.DAILY_MALL_GIFTS.next();
        var change = this.getPlayer().getInventory().addItems(rewards);
        
        // Set and update in database
        this.dailyMallReward = true;
        Nebula.getGameDatabase().update(this, this.getUid(), "dailyMallReward", this.dailyMallReward);
        
        // Trigger quest
        this.getPlayer().trigger(QuestCondition.DailyShopReceiveShopTotal, 1);
        
        // Success
        return change.setSuccess(true);
    }
    
    // Database
    
    @Override
    public void onLoad() {
        boolean shouldSave = false;
        
        // Fix missing weekly quests
        if (this.claimedWeeklyIds == null) {
            this.claimedWeeklyIds = new IntOpenHashSet();
            shouldSave = true;
        }
        
        // Fix quest lists
        if (this.getList() == null) {
            // Create quest list
            this.list = new HashMap<>();
            
            // Add missing Star Tower quests and refresh their condition data
            // We do not need to save here because QuestManager::resetDailyQuests will save to the database
            this.initStarTowerQuests();
            this.initStarTowerBookEventQuests();
            
            // Reset daily quests
            this.resetDailyQuests(true);
            
            // QuestManager::resetDailyQuests will save to the database, so we dont need to set the shouldSave flag
            shouldSave = false;
        } else {
            // Add missing Star Tower quests and refresh their condition data
            boolean changed = this.initStarTowerQuests();
            
            if (this.initStarTowerBookEventQuests()) {
                changed = true;
            }
            
            if (changed) {
                shouldSave = true;
            }
        }
        
        // Save to database
        if (shouldSave) {
            this.save();
        }
    }
    
    /**
     * Persists a single quest to the database
     */
    public void saveQuest(GameQuest quest) {
        Nebula.getGameDatabase().update(this, this.getUid(), "list." + quest.getKey(), quest);
    }
    
    // Serialization
    
    public void encodePlayerInfo(PlayerInfo proto) {
        // Quests proto
        var quests = proto.getMutableQuests();
        
        for (var quest : this.getQuests()) {
            quests.addList(quest.toProto());
        }
        
        // Set claimed activity ids
        for (int id : this.getClaimedActiveIds()) {
            proto.addDailyActiveIds(id);
        }
        
        for (int id : this.getClaimedWeeklyIds()) {
            proto.addWeeklyActiveIds(id);
        }
        
        // Set world level rewards
        proto.getMutableState()
            .getMutableWorldClassReward()
            .setFlag(this.getLevelRewards().toBigEndianByteArray());
        
        // Force claim tour guide
        proto.setTourGuideQuestGroup(9);
    }
}
