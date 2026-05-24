package emu.nebula.game.battlepass;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import emu.nebula.GameConstants;
import emu.nebula.Nebula;
import emu.nebula.data.GameData;
import emu.nebula.data.resources.BattlePassDef;
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
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
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
        this.battlePassId = getActiveBattlePassId();
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

    private static int getActiveBattlePassId() {
        long currentTimeSec = Nebula.getCurrentServerTime();

        for (BattlePassDef data : GameData.getBattlePassDataTable()) {
            if (data == null) {
                continue;
            }

            long start = data.getStartTimeTimestamp();
            long end = data.getEndTimeTimestamp();
            if (currentTimeSec >= start && currentTimeSec <= end) {
                return data.getId();
            }
        }

        return GameConstants.BATTLE_PASS_ID;
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
     * Check claimable tasks to show battle pass red dot.
     */
    public synchronized boolean hasClaimableQuest() {
        if (!this.getPlayer().isBattlePassUnlocked()) {
            return false;
        }

        var nextLevelData = GameData.getBattlePassLevelDataTable().get(this.getLevel() + 1);
        if (nextLevelData == null) {
            return false;
        }

        for (var quest : getQuests().values()) {
            if (quest.isComplete() && !quest.isClaimed()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns whether the battle pass currently has at least one claimable
     * reward lane item for the player's current level/mode.
     */
    public synchronized boolean hasClaimableReward() {
        if (!this.getPlayer().isBattlePassUnlocked()) {
            return false;
        }

        for (int i = 1; i <= this.getLevel(); i++) {
            if (!this.getBasicReward().isSet(i)) {
                return true;
            }

            if (this.isPremium() && !this.getPremiumReward().isSet(i)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Encodes the client battle-pass state red dot contract:
     * 0 = none, 1 = quest only, 2 = reward only, 3 = both.
     */
    public synchronized int getClientState() {
        int state = 0;
        if (this.hasClaimableQuest()) {
            state |= 1;
        }
        if (this.hasClaimableReward()) {
            state |= 2;
        }
        return state;
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
        if (!getPlayer().hasSession() || !getPlayer().isBattlePassUnlocked()) {
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
        Bitset rewards;
        
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
            var premiumRewards = data.getPremiumRewards().clone();

            if (this.getMode() >= 2 && data.hasLuxuryRewards()) {
                premiumRewards.add(data.getLuxuryRewards());
            }

            return getPlayer().getInventory().addItems(premiumRewards);
        } else {
            return getPlayer().getInventory().addItems(data.getBasicRewards());
        }
    }
    
    public PlayerChangeInfo receiveReward() {
        // Init rewards
        var rewards = new ItemParamMap();
        
        // Get unclaimed rewards
        for (int i = 1; i <= this.getLevel(); i++) {
            boolean claimBasic = !this.getBasicReward().isSet(i);
            boolean claimPremium = this.isPremium() && !this.getPremiumReward().isSet(i);

            if (!claimBasic && !claimPremium) {
                continue;
            }

            BattlePassRewardDef data = this.getRewardData(i);
            if (data == null) {
                continue;
            }

            if (claimBasic) {
                this.getBasicReward().setBit(i);
                rewards.add(data.getBasicRewards());
            }

            if (claimPremium) {
                this.getPremiumReward().setBit(i);
                rewards.add(data.getPremiumRewards());

                if (this.getMode() >= 2 && data.hasLuxuryRewards()) {
                    rewards.add(data.getLuxuryRewards());
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
        // Return a locked/empty snapshot until the battle pass feature is unlocked,
        // so the client cannot derive local quest or reward red dots from battle pass data.
        if (!this.getPlayer().isBattlePassUnlocked()) {
            return BattlePassInfo.newInstance()
                    .setId(0)
                    .setLevel(0)
                    .setMode(0)
                    .setExp(0)
                    .setExpThisWeek(0)
                    .setDeadline(0L)
                    .setBasicReward()
                    .setPremiumReward();
        }

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
