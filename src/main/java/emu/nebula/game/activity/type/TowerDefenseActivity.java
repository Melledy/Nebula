package emu.nebula.game.activity.type;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import dev.morphia.annotations.Entity;
import emu.nebula.data.GameData;
import emu.nebula.data.resources.ActivityDef;
import emu.nebula.game.activity.ActivityManager;
import emu.nebula.game.activity.GameActivity;
import emu.nebula.game.inventory.ItemParamMap;
import emu.nebula.game.player.PlayerChangeInfo;
import emu.nebula.proto.ActivityDetail.ActivityMsg;
import emu.nebula.proto.Public.ActivityQuest;
import emu.nebula.proto.Public.ActivityTowerDefenseLevel;
import lombok.Getter;

@Getter
@Entity
public class TowerDefenseActivity extends GameActivity {
    private Map<Integer, Integer> completedStages;
    private Map<Integer, Integer> completedQuests;
    
    @Deprecated // Morphia only
    public TowerDefenseActivity() {
        
    }
    
    public TowerDefenseActivity(ActivityManager manager, ActivityDef data) {
        super(manager, data);
        this.completedStages = new HashMap<Integer, Integer>();
        this.completedQuests = new HashMap<Integer, Integer>();
    }

    public PlayerChangeInfo claimReward(int level) {
        // Initialize change info
        var change = new PlayerChangeInfo();

        // Get rewards
        var rewards = GameData.getTowerDefenseLevelDataTable().get(level).getRewards();

        // Add rewards
        return getPlayer().getInventory().addItems(rewards, change);
    }
    
    // public PlayerChangeInfo claimReward(int groupId) {
    //     // Create change info
    //     var change = new PlayerChangeInfo();
        
    //     // Make sure we haven't completed this group yet
    //     if (this.getCompleted().contains(groupId)) {
    //         return change;
    //     }
        
    //     // Get trial control
    //     var control = GameData.getTrialControlDataTable().get(this.getId());
    //     if (control == null) return change;
        
    //     // Get group
    //     var group = GameData.getTrialGroupDataTable().get(groupId);
    //     if (group == null) return change;
        
    //     // Set as completed
    //     this.getCompleted().add(groupId);
        
    //     // Save to database
    //     this.save();
        
    //     // Add rewards
    //     return getPlayer().getInventory().addItems(group.getRewards(), change);
    // }
    
    // Proto

    @Override
    public void encodeActivityMsg(ActivityMsg msg) {
        var proto = msg.getMutableTowerDefense();

        // Add completed stages
        for (int id : this.completedStages.keySet()) {
            // Create proto
            var level = ActivityTowerDefenseLevel.newInstance();

            // Set proto params
            level.setId(id);
            level.setStar(this.completedStages.get(id));

            // Add to final msg proto
            proto.addLevels(level);
        }

        // Add completed quests
        for (int id : this.completedStages.keySet()) {
            // Create proto
            var quest = ActivityQuest.newInstance();

            // Set proto params
            quest.setActivityId(this.getId());
            quest.setId(id);
            quest.setStatus(2);     // TODO: properly handle event quests

            // Add to final msg proto
            proto.addQuests(quest);
        }
    }

}
