package emu.nebula.game.activity.type;

import dev.morphia.annotations.Entity;
import emu.nebula.data.GameData;
import emu.nebula.data.resources.ActivityDef;
import emu.nebula.game.activity.ActivityManager;
import emu.nebula.game.activity.GameActivity;
import emu.nebula.game.inventory.ItemParamMap;
import emu.nebula.game.player.PlayerChangeInfo;
import emu.nebula.proto.ActivityDetail.ActivityMsg;

import lombok.Getter;

@Getter
@Entity
public class LoginRewardActivity extends GameActivity {
    private int actual;     // Received rewards
    private int receive;    // Rewards level that we can claim
    
    private boolean complete;
    private long lastEpochDay;
    
    @Deprecated // Morphia only
    public LoginRewardActivity() {
        
    }
    
    public LoginRewardActivity(ActivityManager manager, ActivityDef data) {
        super(manager, data);
    }
    
    public synchronized PlayerChangeInfo claim() {
        // Create change info
        var change = new PlayerChangeInfo();
        
        // Sanity check
        if (this.getActual() >= this.getReceive()) {
            return change;
        }
        
        // Get rewards
        var rewards = new ItemParamMap();
        
        for (int i = this.getActual() + 1; i <= this.getReceive(); i++) {
            int rewardId = (this.getId() * 100) + i;
            var data = GameData.getLoginRewardGroupControlDataTable().get(rewardId);
            
            if (data == null) {
                continue;
            }
            
            rewards.add(data.getRewards());
        }
        
        // Give rewards to player
        this.getPlayer().getInventory().addItems(rewards, change);
        
        // Update claimed and save to database
        this.actual = this.receive;
        this.save();
        
        // Complete
        return change;
    }
    
    @Override
    public synchronized void onLogin() {
        // Check if epoch day has changed
        if (this.getPlayer().getLastEpochDay() <= this.lastEpochDay || this.isComplete()) {
            return;
        }
        
        // Get next reward
        int nextReceive = this.receive + 1;
        int rewardId = (this.getId() * 100) + nextReceive;
        var data = GameData.getLoginRewardGroupControlDataTable().get(rewardId);
        
        if (data != null) {
            this.receive = nextReceive;
        } else {
            this.complete = true; // Set "complete" flag so we dont need to save to database/do a recheck every login
        }
                
        // Unlock next reward
        this.lastEpochDay = this.getPlayer().getLastEpochDay();
        
        // Save
        this.save();
    }
    
    // Proto

    @Override
    public void encodeActivityMsg(ActivityMsg msg) {
        msg.getMutableLogin()
            .setActivityId(this.getId())
            .setActual(this.getActual())
            .setReceive(this.getReceive());
    }

}
