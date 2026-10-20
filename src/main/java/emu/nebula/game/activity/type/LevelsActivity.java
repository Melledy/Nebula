package emu.nebula.game.activity.type;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import dev.morphia.annotations.Entity;

import emu.nebula.GameConstants;
import emu.nebula.data.GameData;
import emu.nebula.data.resources.ActivityDef;
import emu.nebula.data.resources.ActivityLevelsLevelDef;
import emu.nebula.game.activity.ActivityManager;
import emu.nebula.game.activity.GameActivity;
import emu.nebula.game.instance.InstanceSettleData;
import emu.nebula.game.inventory.ItemParamMap;
import emu.nebula.game.player.PlayerChangeInfo;
import emu.nebula.game.quest.QuestCondition;
import emu.nebula.proto.ActivityDetail.ActivityMsg;
import emu.nebula.proto.Public.ActivityLevel;
import emu.nebula.proto.Public.UseBuild;
import lombok.Getter;
import lombok.Setter;

@Getter
@Entity
public class LevelsActivity extends GameActivity {
    private Map<Integer, ActivityLevelInfo> levels;
    
    // Apply level data
    private transient ActivityLevelsLevelDef level;
    private transient long buildId;
    
    @Deprecated // Morphia only
    public LevelsActivity() {
        
    }
    
    public LevelsActivity(ActivityManager manager, ActivityDef data) {
        super(manager, data);
        this.levels = new HashMap<>();
    }

    public boolean apply(int levelId, long buildId) {
        // Verify level
        var level = GameData.getActivityLevelsLevelDataTable().get(levelId);
        
        if (level == null || level.getActivityId() != this.getId()) {
            return false;
        }
        
        // Verify build
        var build = this.getPlayer().getStarTowerManager().getBuildById(buildId);
        
        if (build == null) {
            return false;
        }
        
        // Set
        this.level = level;
        this.buildId = buildId;
        
        // Success
        return true;
    }
    
    public PlayerChangeInfo settle(int star) {
        // Check energy
        if (!this.getLevel().hasEnergy(this.getPlayer(), 1)) {
            return null;
        }
        
        // Calculate settle data
        var settleData = new InstanceSettleData();
        
        settleData.setWin(star > 0);
        settleData.setFirst(settleData.isWin() && !this.getLevels().containsKey(this.getLevel().getId()));
        
        // Init player change info
        var change = new PlayerChangeInfo();
        
        // Handle win
        if (settleData.isWin()) {
            // Calculate energy and exp
            settleData.setExp(this.getLevel().getEnergyConsume());
            getPlayer().consumeEnergy(settleData.getExp(), change);
            
            // Calculate rewards
            settleData.generateRewards(this.getLevel());
            
            // Add to inventory
            getPlayer().getInventory().addItem(GameConstants.EXP_ITEM_ID, settleData.getExp(), change);
            getPlayer().getInventory().addItems(settleData.getRewards(), change);
            getPlayer().getInventory().addItems(settleData.getFirstRewards(), change);
            
            // Log
            var level = getLevels().computeIfAbsent(this.getLevel().getId(), i -> new ActivityLevelInfo());
            
            level.setStar(star);
            level.setBuildId(this.getBuildId());
            
            // Save to database
            this.save();
            
            // Quest triggers
            this.getPlayer().trigger(QuestCondition.BattleTotal, 1);
        }
        
        // Set extra data
        change.setExtraData(settleData);
        
        // Success
        return change;
    }

    public PlayerChangeInfo sweep(ActivityLevelsLevelDef data, int count) {
        // Sanity check count
        if (count <= 0) {
            return null;
        }
        
        // Check if we have 3 starred this instance
        var level = this.getLevels().get(data.getId());
        
        if (level == null || level.getStar() != 3) {
            return null;
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
            var rewards = data.getRewards().generate();
            
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
        this.getPlayer().trigger(QuestCondition.BattleTotal, count);
        
        // Success
        return change.setSuccess(true);
    }
    
    // Proto

    @Override
    public void encodeActivityMsg(ActivityMsg msg) {
        var proto = msg.getMutableLevels();
        
        for (var entry : this.getLevels().entrySet()) {
            int id = entry.getKey();
            var level = entry.getValue();
            
            var info = level.toProto()
                    .setId(id);
            
            proto.addLevels(info);
        }
    }

    @Setter
    @Getter
    @Entity(useDiscriminator = false)
    public static class ActivityLevelInfo {
        private int star;
        private long buildId;
        
        public ActivityLevelInfo() {
            
        }
        
        // Proto
        
        public ActivityLevel toProto() {
            var proto = ActivityLevel.newInstance()
                    .setStar(this.getStar())
                    .setBuild(UseBuild.newInstance().setBuildId(this.getBuildId()).setTypeValue(1));
            
            return proto;
        }
    }
}
