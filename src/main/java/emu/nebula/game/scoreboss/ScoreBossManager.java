package emu.nebula.game.scoreboss;

import java.util.Collection;
import java.util.List;

import emu.nebula.Nebula;
import emu.nebula.data.GameData;
import emu.nebula.data.resources.ScoreBossControlDef;
import emu.nebula.data.resources.ScoreBossRewardDef;
import emu.nebula.game.inventory.ItemParamMap;
import emu.nebula.game.player.Player;
import emu.nebula.game.player.PlayerChangeInfo;
import emu.nebula.game.player.PlayerManager;

import lombok.Getter;

@Getter
public class ScoreBossManager extends PlayerManager  {
    private int levelId;
    private long buildId;
    
    private boolean checkedDatabase;
    private ScoreBossRankEntry ranking;
    
    public ScoreBossManager(Player player) {
        super(player);
    }
    
    public int getControlId() {
        return Nebula.getGameContext().getScoreBossModule().getControlId();
    }
    
    public ScoreBossControlDef getControlData() {
        return GameData.getScoreBossControlDataTable().get(this.getControlId());
    }
    
    public ScoreBossRankEntry getRankEntry() {
        if (this.ranking == null && !this.checkedDatabase) {
            this.ranking = Nebula.getGameDatabase().getObjectByUid(ScoreBossRankEntry.class, this.getPlayerUid());
            this.checkedDatabase = true;
        }
        
        return this.ranking;
    }
    
    public boolean apply(int levelId, long buildId) {
        // Get level
        var control = getControlData();
        if (control == null || !control.getLevelGroup().contains(levelId)) {
            return false;
        }
        
        // Get build
        var build = this.getPlayer().getStarTowerManager().getBuildById(buildId);
        if (build == null) {
            return false;
        }
        
        // Set
        this.levelId = levelId;
        this.buildId = buildId;
        
        // Success
        return true;
    }

    public boolean settle(int stars, int score, int skillScore) {
        // Get level
        var control = getControlData();
        if (control == null || !control.getLevelGroup().contains(this.getLevelId())) {
            return false;
        }
        
        // Get build
        var build = getPlayer().getStarTowerManager().getBuildById(this.getBuildId());
        if (build == null) {
            return false;
        }
        
        // Get ranking from database
        this.getRankEntry();
        
        // Create ranking if its not in the database
        if (this.ranking == null) {
            this.ranking = new ScoreBossRankEntry(this.getPlayer(), this.getControlId());
        }
        
        // Settle
        this.ranking.settle(this.getPlayer(), build, getControlId(), getLevelId(), stars, score, skillScore);
        
        // Save ranking
        this.ranking.save();
        
        // Clear
        this.levelId = 0;
        this.buildId = 0;
        
        // Success
        return true;
    }

    public PlayerChangeInfo claimRewards(int star) {
        // Get rank entry
        this.getRankEntry();
        
        if (this.ranking == null) {
            return new PlayerChangeInfo();
        }
        
        // Init variables
        Collection<ScoreBossRewardDef> claims = null;
        
        // Add to claim list
        if (star > 0) {
            var data = GameData.getScoreBossRewardDataTable().get(star);
            if (data != null) {
                claims = List.of(data);
            }
        } else {
            claims = GameData.getScoreBossRewardDataTable().values();
        }
        
        // Init rewards
        var rewards = new ItemParamMap();
        int starCount = ranking.getStars();
        boolean shouldSave = false;
        
        // Try and claim
        for (var data : claims) {
            // Check if we have earned enough stars
            if (starCount >= data.getStarNeed() && !ranking.getClaimedRewards().contains(data.getId())) {
                // Add rewards
                rewards.add(data.getRewardItemId1(), data.getRewardNum1());
                
                // Set in claimed rewards
                this.ranking.getClaimedRewards().add(data.getId());
                
                // Set save flag so we update ranking to the database
                shouldSave = true;
            }
        }
        
        // Save to database
        if (shouldSave) {
            this.ranking.save();
        }
        
        // Add rewards
        return this.getPlayer().getInventory().addItems(rewards);     
    }
}
