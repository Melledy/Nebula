package emu.nebula.game.activity.type;

import dev.morphia.annotations.Entity;

import emu.nebula.data.GameData;
import emu.nebula.data.resources.ActivityDef;
import emu.nebula.game.activity.ActivityManager;
import emu.nebula.game.activity.GameActivity;
import emu.nebula.game.player.PlayerChangeInfo;
import emu.nebula.proto.ActivityDetail.ActivityMsg;
import emu.nebula.proto.Public.ActivityQuest;
import emu.nebula.proto.Public.ActivityTowerDefenseLevel;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import lombok.Getter;

@Getter
@Entity
public class PenguinCardActivity extends GameActivity {
    private Int2IntMap completedStages;
    private Int2IntMap completedQuests;
    
    @Deprecated // Morphia only
    public PenguinCardActivity() {
        
    }
    
    public PenguinCardActivity(ActivityManager manager, ActivityDef data) {
        super(manager, data);
        this.completedStages = new Int2IntOpenHashMap();
        this.completedQuests = new Int2IntOpenHashMap();
    }

    public PlayerChangeInfo claimReward(int level) {
        // Get rewards
        var rewards = GameData.getTowerDefenseLevelDataTable().get(level).getRewards();

        // Add rewards
        return getPlayer().getInventory().addItems(rewards);
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
        msg.getMutablePenguinCard();
    }

}
