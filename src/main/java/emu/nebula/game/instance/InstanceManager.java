package emu.nebula.game.instance;

import java.util.ArrayList;

import emu.nebula.GameConstants;
import emu.nebula.game.inventory.ItemParamMap;
import emu.nebula.game.player.Player;
import emu.nebula.game.player.PlayerChangeInfo;
import emu.nebula.game.player.PlayerManager;
import emu.nebula.game.player.PlayerProgress;
import emu.nebula.game.quest.QuestCondition;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import lombok.Getter;

@Getter
public class InstanceManager extends PlayerManager {
    private int curInstanceId;
    private int rewardType;
    
    public InstanceManager(Player player) {
        super(player);
    }
    
    public void setCurInstanceId(int id) {
        this.setCurInstanceId(id, 0);
    }

    public void setCurInstanceId(int id, int rewardType) {
        this.curInstanceId = id;
        this.rewardType = rewardType;
    }
    
    private PlayerProgress getProgress() {
        return this.getPlayer().getProgress();
    }
    
    public PlayerChangeInfo settleInstance(InstanceData data, QuestCondition questCondition, Int2IntMap log, String logName, int star) {
        // Calculate settle data
        var settleData = new InstanceSettleData();
        
        settleData.setWin(star > 0);
        settleData.setFirst(settleData.isWin() && !log.containsKey(data.getId()));
        
        // Init player change info
        var change = new PlayerChangeInfo();
        
        // Handle win
        if (settleData.isWin()) {
            // Calculate energy and exp
            settleData.setExp(data.getEnergyConsume());
            getPlayer().consumeEnergy(settleData.getExp(), change);
            
            // Calculate rewards
            settleData.generateRewards(data, this.getRewardType());
            
            // Add to inventory
            getPlayer().getInventory().addItem(GameConstants.EXP_ITEM_ID, settleData.getExp(), change);
            getPlayer().getInventory().addItems(settleData.getRewards(), change);
            getPlayer().getInventory().addItems(settleData.getFirstRewards(), change);
            
            // Log
            this.getProgress().saveInstanceLog(log, logName, data.getId(), star);
            
            // Quest triggers
            this.getPlayer().trigger(questCondition, 1);
            this.getPlayer().trigger(QuestCondition.BattleTotal, 1);
        }
        
        // Set extra data
        change.setExtraData(settleData);
        
        // Clear instance
        this.curInstanceId = 0;
        this.rewardType = 0;
        
        // Success
        return change.setSuccess(true);
    }
    
    public PlayerChangeInfo sweepInstance(InstanceData data, QuestCondition questCondition, Int2IntMap log, int rewardType, int count) {
        // Sanity check count
        if (count <= 0) {
            return null;
        }
        
        // Check if we have 3 starred this instance
        int stars = log.get(data.getId());
        
        if (rewardType > 0) {
            // Daily instance
            if (stars != 7) {
                return null;
            }
        } else {
            // Other instances
            if (stars != 3) {
                return null;
            }
        }
        
        // Check energy cost
        int energyCost = data.getEnergyConsume() * count;
        
        if (this.getPlayer().getEnergy() < energyCost) {
            return null;
        }
        
        // Init variables
        var change = new PlayerChangeInfo();
        var list = new ArrayList<ItemParamMap>();
        
        // Consume exp
        getPlayer().consumeEnergy(energyCost, change);
        getPlayer().getInventory().addItem(GameConstants.EXP_ITEM_ID, energyCost, change);
        
        // Calculate total rewards
        var totalRewards = new ItemParamMap();
        
        for (int i = 0; i < count; i++) {
            // Generate rewards for each settle count
            var rewards = data.getRewards(rewardType).generate();
            
            // Add to reward list
            list.add(rewards);
            
            // Add to total rewards
            totalRewards.add(rewards);
        }
        
        // Add total rewards to inventory
        getPlayer().getInventory().addItems(totalRewards, change);
        
        // Set reward list in change info so we can serialize it in the response proto later
        change.setExtraData(list);
        
        // Quest triggers
        this.getPlayer().trigger(questCondition, count);
        this.getPlayer().trigger(QuestCondition.BattleTotal, count);
        
        // Success
        return change.setSuccess(true);
    }
    
    public PlayerChangeInfo settleWeekly(InstanceData data, boolean win) {
        // Calculate settle data
        var settleData = new InstanceSettleData();
        
        settleData.setWin(win);
        settleData.setFirst(settleData.isWin() && !getProgress().getWeekBossLog().containsKey(data.getId()));
        
        // Init player change info
        var change = new PlayerChangeInfo();
        
        // Handle win
        if (settleData.isWin()) {
            // Calculate rewards
            int entries = this.getPlayer().getInventory().getResourceCount(GameConstants.WEEKLY_ENTRY_ITEM_ID);
            var rewards = new ItemParamMap();
            
            if (entries > 0) {
                // Generate regular rewards
                rewards.add(data.getRewards().generate());
                
                // Remove weekly entry
                getPlayer().getInventory().removeItem(GameConstants.WEEKLY_ENTRY_ITEM_ID, 1, change);
            }
            
            // Add first clear rewards even if we dont have the entry ticket
            if (settleData.isFirst()) {
                // Generate first clear rewards
                rewards.add(data.getFirstRewards().generate());
            }
            
            // Calculate rewards if we have any
            if (!rewards.isEmpty()) {
                // Set rewards in settle data
                settleData.setRewards(rewards);
                
                // Add rewards to inventory
                getPlayer().getInventory().addItems(rewards, change);
            }
            
            // Log
            this.getProgress().saveInstanceLog(getProgress().getWeekBossLog(), "weekBossLog", data.getId(), 1);
            
            // Quest triggers
            this.getPlayer().trigger(QuestCondition.WeekBoosClearSpecificDifficultyAndTotal, 1);
            this.getPlayer().trigger(QuestCondition.WeekBossClearTotal, 1);
            this.getPlayer().trigger(QuestCondition.BattleTotal, 1);
        }
        
        // Set extra data
        change.setExtraData(settleData);
        
        // Success
        return change.setSuccess(true);
    }

}
