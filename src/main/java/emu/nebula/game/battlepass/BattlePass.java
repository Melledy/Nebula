package emu.nebula.game.battlepass;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;

import emu.nebula.GameConstants;
import emu.nebula.Nebula;
import emu.nebula.data.GameData;
import emu.nebula.data.resources.BattlePassRewardDef;
import emu.nebula.database.GameDatabaseObject;
import emu.nebula.game.inventory.ItemParamMap;
import emu.nebula.game.player.Player;
import emu.nebula.game.player.PlayerChangeInfo;
import emu.nebula.game.quest.GameQuest;
import emu.nebula.game.quest.QuestType;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.BattlePassInfoOuterClass.BattlePassInfo;
import emu.nebula.util.Bitset;

import lombok.Getter;

@Getter
@Entity(value = "battlepass", useDiscriminator = false)
public class BattlePass implements GameDatabaseObject {
    @Id
    private int uid;
    private transient BattlePassManager manager;
    
    private int battlePassId; // Season id
    private int mode;
    private int level;
    private int exp;
    private int expWeek;
    
    private Bitset basicReward;
    private Bitset premiumReward;
    
    private Map<Integer, GameQuest> quests;
    
    @Deprecated // Morphia only
    public BattlePass() {
        
    }
    
    public BattlePass(BattlePassManager manager) {
        this.uid = manager.getPlayerUid();
        this.manager = manager;
        this.battlePassId = GameConstants.BATTLE_PASS_ID;
        this.basicReward = new Bitset();
        this.premiumReward = new Bitset();
        
        // Setup battle pass quests
        this.quests = new HashMap<>();
        for (var data : GameData.getBattlePassQuestDataTable()) {
            this.quests.put(data.getId(), new GameQuest(data));
        }
        
        // Save to database
        this.save();
    }

    public void setManager(BattlePassManager manager) {
        this.manager = manager;
    }
    
    public Player getPlayer() {
        return manager.getPlayer();
    }
    
    /**
     * Sets the mode directly
     */
    public synchronized void setMode(int mode) {
        this.mode = mode;
    }
    
    public boolean isPremium() {
        return this.mode > 0;
    }

    private BattlePassRewardDef getRewardData(int level) {
        return GameData.getBattlePassRewardDataTable().get((this.getBattlePassId() << 16) + level);
    }
    
    /**
     * Sets the level directly, use getMaxExp() instead if adding exp.
     */
    public synchronized void setLevel(int level) {
        this.level = level;
        this.exp = 0;
    }
    
    public int getMaxExp() {
        var data = GameData.getBattlePassLevelDataTable().get(this.getLevel() + 1);
        return data != null ? data.getExp() : 0; 
    }
    
    public void addExp(int amount) {
        // Setup
        int expRequired = this.getMaxExp();

        // Add exp
        this.exp += amount;

        // Check for level ups
        while (this.exp >= expRequired && expRequired > 0) {
            this.level += 1;
            this.exp -= expRequired;
            
            expRequired = this.getMaxExp();
        }
    }
    
    /**
     * Returns true if any rewards or quests are claimable
     */
    public synchronized boolean hasNew() {
        // Check if any quests are complete but unclaimed
        for (var quest : getQuests().values()) {
            if (quest.isComplete() && !quest.isClaimed()) {
                return true;
            }
        }
        
        // Check if we have any pending rewards
        for (int i = 1; i <= this.getLevel(); i++) {
            if (!this.getBasicReward().isSet(i)) {
                return true;
            }
            
            if (this.isPremium() && !this.getPremiumReward().isSet(i)) {
                return true;
            }
        }
        
        // No claimable things
        return false;
    }
    
    public synchronized void resetDailyQuests(boolean resetWeekly) {
        // Reset daily quests
        for (var data : GameData.getBattlePassQuestDataTable()) {
            // Get quest
            var quest = getQuests().computeIfAbsent(data.getId(), i -> new GameQuest(data));
            
            // Don't reset weekly quests
            if (!data.isDaily() && !resetWeekly) {
                continue;
            }
            
            // Reset progress
            quest.resetProgress();
            
            // Sync quest with player client
            this.syncQuest(quest);
        }
        
        // Reset weekly limit for exp
        if (resetWeekly) {
            this.expWeek = 0;
        }
        
        // Persist to database
        this.save();
    }
    
    public synchronized void trigger(int condition, int progress, int param1, int param2) {
        for (var quest : getQuests().values()) {
            // Try to trigger quest
            boolean result = quest.trigger(condition, progress, param1, param2);
            
            // Skip if quest progress wasn't changed
            if (!result) {
                continue;
            }
            
            // Sync quest with player client
            this.syncQuest(quest);
            
            // Update in database
            Nebula.getGameDatabase().update(this, this.getUid(), "quests." + quest.getId(), quest);
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
    
    public BattlePass receiveQuestReward(int questId) {
        // Get received quests
        var claimList = new ArrayList<GameQuest>();
        
        if (questId > 0) {
            // Claim specific quest
            var quest = this.getQuests().get(questId);
            
            if (quest != null && !quest.isClaimed()) {
                claimList.add(quest);
            }
        } else {
            // Claim all
            for (var quest : this.getQuests().values()) {
                if (!quest.isComplete() || quest.isClaimed()) {
                    continue;
                }
                
                claimList.add(quest);
            }
        }
        
        // Sanity check
        if (claimList.isEmpty()) {
            return null;
        }
        
        // Init exp
        int exp = 0;
        int expWeek = 0;
        
        // Claim
        for (var quest : claimList) {
            // Get data
            var data = GameData.getBattlePassQuestDataTable().get(quest.getId());
            if (data == null) {
                continue;
            }
            
            // Set claimed
            quest.setClaimed(true);
            
            // Add exp
            exp += data.getExp();
            
            // Check if quest is weekly
            if (quest.getType() == QuestType.BattlePassWeekly) {
                expWeek += data.getExp();
            }
        }
        
        // Add exp
        if (exp > 0) {
            this.addExp(exp);
        }
        
        if (expWeek > 0) {
            this.expWeek += expWeek;
        }
        
        // Save to database
        this.save();
        
        // Success
        return this;
    }
    
    public PlayerChangeInfo receiveReward(boolean premium, int levelId) {
        // Get bitset
        Bitset rewards = null;
        
        if (premium) {
            rewards = this.getPremiumReward();
        } else {
            rewards = this.getBasicReward();
        }
        
        // Make sure we haven't already claimed the reward
        if (rewards.isSet(levelId)) {
            return null;
        }
        
        // Set claimed
        rewards.setBit(levelId);
        
        // Save to database
        this.save();
        
        // Get reward data
        var data = this.getRewardData(levelId);
        if (data == null) {
            return new PlayerChangeInfo();
        }
        
        // Add items
        if (premium) {
            return getPlayer().getInventory().addItems(data.getPremiumRewards());
        } else {
            return getPlayer().getInventory().addItems(data.getBasicRewards());
        }
    }
    
    public PlayerChangeInfo receiveReward() {
        // Init rewards
        var rewards = new ItemParamMap();
        
        // Get unclaimed rewards
        for (int i = 1; i <= this.getLevel(); i++) {
            // Cache reward data
            BattlePassRewardDef data = null;
            
            // Basic reward
            if (!this.getBasicReward().isSet(i)) {
                // Set flag
                this.getBasicReward().setBit(i);
                
                // Get reward data if we havent already
                if (data == null) {
                    data = this.getRewardData(i);
                }
                
                // Add basic rewards
                if (data != null) {
                    rewards.add(data.getBasicRewards());
                }
            }
            
            // Premium reward
            if (this.isPremium() && !this.getPremiumReward().isSet(i)) {
                // Set flag
                this.getPremiumReward().setBit(i);
                
                // Get reward data if we havent already
                if (data == null) {
                    data = this.getRewardData(i);
                }
                
                // Add basic rewards
                if (data != null) {
                    rewards.add(data.getPremiumRewards());
                }
            }
        }
        
        // Save if we have any rewards to add
        if (rewards.size() > 0) {
            this.save();
        } else {
            return null;
        }
        
        // Add rewards
        return getPlayer().getInventory().addItems(rewards);
    }

    // Proto
    
    public BattlePassInfo toProto() {
        var proto = BattlePassInfo.newInstance()
                .setId(this.getBattlePassId())
                .setLevel(this.getLevel())
                .setMode(this.getMode())
                .setExp(this.getExp())
                .setExpThisWeek(this.getExpWeek())
                .setDeadline(Long.MAX_VALUE)
                .setBasicReward(this.getBasicReward().toByteArray())
                .setPremiumReward(this.getPremiumReward().toByteArray());
        
        var daily = proto.getMutableDailyQuests();
        var weekly = proto.getMutableWeeklyQuests();
        
        for (var quest : this.getQuests().values()) {
            if (quest.getType() == QuestType.BattlePassDaily) {
                daily.addList(quest.toProto());
            } else if (quest.getType() == QuestType.BattlePassWeekly) {
                weekly.addList(quest.toProto());
            }
        }
        
        return proto;
    }
}
