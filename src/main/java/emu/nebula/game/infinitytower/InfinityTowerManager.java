package emu.nebula.game.infinitytower;

import emu.nebula.data.GameData;
import emu.nebula.data.resources.InfinityTowerLevelDef;
import emu.nebula.game.achievement.AchievementCondition;
import emu.nebula.game.player.Player;
import emu.nebula.game.player.PlayerChangeInfo;
import emu.nebula.game.player.PlayerManager;
import emu.nebula.game.player.PlayerProgress;
import lombok.Getter;

@Getter
public class InfinityTowerManager extends PlayerManager {
    private InfinityTowerLevelDef levelData;
    private int levelId;
    private long buildId;
    
    public InfinityTowerManager(Player player) {
        super(player);
    }

    private PlayerProgress getProgress() {
        return this.getPlayer().getProgress();
    }
    
    public int getBountyLevel() {
        return 0;
    }
    
    public boolean apply(int levelId, long buildId) {
        // Verify level data
        var data = GameData.getInfinityTowerLevelDataTable().get(levelId);
        if (data == null) {
            return false;
        }
        
        // Set level id
        this.levelData = data;
        this.levelId = levelId;
        
        // Set build id
        if (buildId >= 0) {
            this.buildId = buildId;
        }
        
        // Success
        return true;
    }

    public PlayerChangeInfo settle(int value) {
        // Verify level data
        if (this.getLevelData() == null) {
            return null;
        }
        
        // Init change info
        var change = new PlayerChangeInfo();
        
        // Check if the player has won or not TODO
        if (value != 1) {
            // Player lost, so we return nothing
            return change;
        }
        
        // Check logs if the player has completed the level already
        int highestLevel = this.getProgress().getInfinityTowerLog().get(this.getLevelData().getTowerId());
        if (highestLevel >= this.getLevelId()) {
            return change;
        }
        
        // Calculate rewards
        var rewards = this.getLevelData().generateRewards();
        
        // Add items to player
        this.getPlayer().getInventory().addItems(rewards, change);
        
        // Set rewards in change info
        change.setExtraData(rewards);
        
        // Log in player progress
        this.getPlayer().getProgress().addInfinityTowerLog(this.getLevelData());
        
        // Trigger achievement
        this.getPlayer().trigger(AchievementCondition.InfinityTowerClearSpecificFloor, 10, this.getLevelId(), 0);
        
        // Clear instance
        this.levelId = 0;
        this.buildId = 0;
        
        // Success
        return change.setSuccess(true);
    }

}
