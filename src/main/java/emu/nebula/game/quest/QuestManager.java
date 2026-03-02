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
        
        this.resetDailyQuests(true);
        
        this.save();
    }
    
    public synchronized GameQuest getQuest(int type, int questId) {
        long key = ((long) type << 32) + questId;
        return this.list.get(key);
    }
    
    public synchronized GameQuest addQuest(GameQuest quest) {
        this.list.put(quest.getKey(), quest);
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
            
            // Try to trigger quest
            boolean result = quest.trigger(condition, progress, param1, param2);
            
            // Skip if quest progress wasn't changed
            if (!result) {
                continue;
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
    private void syncQuest(GameQuest quest) {
        if (!getPlayer().hasSession()) {
            return;
        }
        
        getPlayer().addNextPackage(
            NetMsgId.quest_change_notify, 
            quest.toProto()
        );
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
            Nebula.getGameDatabase().update(this, this.getUid(), "list." + quest.getKey(), quest);
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
            Nebula.getGameDatabase().update(this, this.getUid(), "list." + quest.getKey(), quest);
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
    
    // Daily shop reward
    
    public PlayerChangeInfo claimDailyShopGift() {
        // Sanity check
        if (!this.hasDailyShopReward()) {
            return null;
        }
        
        // Daily shop reward
        var reward = GameConstants.DAILY_GIFTS.next();
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
        var reward = GameConstants.DAILY_GIFTS.next();
        var change = this.getPlayer().getInventory().addItem(reward.getId(), reward.getCount());
        
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
            this.list = new HashMap<>();
            this.resetDailyQuests(true);
            
            // QuestManager::resetDailyQuests will save to the database, so we dont need to set the shouldSave flag
            shouldSave = false;
        }
        
        // Save to database
        if (shouldSave) {
            this.save();
        }
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
