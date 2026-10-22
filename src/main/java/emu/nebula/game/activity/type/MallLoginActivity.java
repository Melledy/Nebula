package emu.nebula.game.activity.type;

import dev.morphia.annotations.Entity;

import emu.nebula.data.GameData;
import emu.nebula.data.resources.ActivityDef;
import emu.nebula.game.activity.ActivityManager;
import emu.nebula.game.activity.GameActivity;
import emu.nebula.game.inventory.ItemParamMap;
import emu.nebula.game.player.PlayerChangeInfo;
import emu.nebula.proto.ActivityDetail.ActivityMsg;
import emu.nebula.util.Utils;

import lombok.Getter;

/**
 * Mall check-in activity
 * <p>
 * These activities are shown inside of the mall instead of the regular activity list and
 * they use their own protocol message (ActivityMallLogin). Next to the amount of unlocked
 * check-in days and the amount of claimed rewards, that message also carries an "active"
 * (purchased) flag which tells the client if the check-in can be claimed at all.
 * <p>
 * The rewards are the same as the ones used by the login reward activities, they are stored
 * in the LoginRewardGroup table using the activity id as a prefix (activityId * 100 + day).
 */
@Getter
@Entity
public class MallLoginActivity extends GameActivity {
    // Whether the check-in is active for this player
    private boolean active;
    // Amount of check-in days that have been unlocked
    private int loginDays;
    // Amount of check-in rewards that have been claimed
    private int received;
    // Last day the check-in was advanced on
    private long lastEpochDay;
    
    @Deprecated // Morphia only
    public MallLoginActivity() {
        
    }
    
    public MallLoginActivity(ActivityManager manager, ActivityDef data) {
        super(manager, data);
    }
    
    // Events
    
    @Override
    public synchronized void onLogin() {
        long epochDay = Utils.getResetEpochDay();
        
        // Check if a new day started since the last login
        if (epochDay <= this.lastEpochDay) {
            return;
        }
        
        // Unlock the next check-in day
        int nextLoginDay = this.loginDays + 1;
        var data = GameData.getLoginRewardGroupControlDataTable().get((this.getId() * 100) + nextLoginDay);
        
        if (data != null) {
            this.loginDays = nextLoginDay;
        }
        
        // Update the last day and save to database
        this.lastEpochDay = epochDay;
        this.save();
    }
    
    // Rewards
    
    public synchronized PlayerChangeInfo claim() {
        // Create change info
        var change = new PlayerChangeInfo();
        
        // Sanity check
        if (!this.isActive() || this.getReceived() >= this.getLoginDays()) {
            return change;
        }
        
        // Get rewards
        var rewards = new ItemParamMap();
        
        for (int i = this.getReceived() + 1; i <= this.getLoginDays(); i++) {
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
        this.received = this.loginDays;
        this.save();
        
        // Complete
        return change;
    }
    
    // Proto
    
    @Override
    public void encodeActivityMsg(ActivityMsg msg) {
        msg.getMutableMallLogin()
            .setActivityId(this.getId())
            .setActive(this.isActive())
            .setLoginDays(this.getLoginDays())
            .setReceived(this.getReceived());
    }
    
}
