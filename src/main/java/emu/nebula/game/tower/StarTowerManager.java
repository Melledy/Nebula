package emu.nebula.game.tower;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import emu.nebula.Nebula;
import emu.nebula.data.GameData;
import emu.nebula.database.GameDatabaseObject;
import emu.nebula.game.player.Player;
import emu.nebula.game.player.PlayerChangeInfo;
import emu.nebula.game.player.PlayerManager;
import emu.nebula.proto.StarTowerApply.StarTowerApplyReq;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import lombok.Getter;

@Getter
@Entity(value = "star_tower", useDiscriminator = false)
public class StarTowerManager extends PlayerManager implements GameDatabaseObject {
    @Id
    private int uid;
    
    // TODO add tower talents here
    
    // Tower game instance
    private transient StarTowerGame game;
    
    // Tower builds
    private transient Long2ObjectMap<StarTowerBuild> builds;
    private transient StarTowerBuild lastBuild;
    
    @Deprecated // Morphia only
    public StarTowerManager() {
        
    }
    
    public StarTowerManager(Player player) {
        super(player);
        this.uid = player.getUid();
        
        this.save();
    }
    
    public Long2ObjectMap<StarTowerBuild> getBuilds() {
        if (this.builds == null) {
            this.loadFromDatabase();
        }
        
        return builds;
    }
    
    public StarTowerBuild getBuildById(long id) {
        return this.getBuilds().get(id);
    }
    
    public StarTowerGame apply(StarTowerApplyReq req) {
        // Sanity checks
        var data = GameData.getStarTowerDataTable().get(req.getId());
        if (data == null) {
            return null;
        }
        
        // Get formation
        var formation = getPlayer().getFormations().getFormationById(req.getFormationId());
        if (formation == null) {
            return null;
        }
        
        // Make sure player has at least 3 chars and 3 discs
        if (formation.getCharCount() != 3 || formation.getDiscCount() < 3) {
            return null;
        }
        
        // Create game
        this.game = new StarTowerGame(this, data, formation, req);
        
        // Success
        return this.game;
    }

    public StarTowerGame endGame() {
        // Cache instance
        var game = this.game;
        
        if (game != null) {
            // Set last build
            this.lastBuild = game.getBuild();
            
            // Clear instance
            this.game = null;
        }
        
        return game;
    }
    
    public boolean saveBuild(boolean delete, String name, boolean lock) {
        // Sanity check
        if (this.getLastBuild() == null) {
            return false;
        }
        
        // Cache build and clear reference
        var build = this.lastBuild;
        this.lastBuild = null;
        
        // Check if the player wants this build or not
        if (delete) {
            return true;
        }
        
        // Check limit
        if (this.getBuilds().size() >= 50) {
            return false;
        }
        
        // Add to builds
        this.getBuilds().put(build.getUid(), build);
        
        // Save build to database
        build.save();
        
        //
        return true;
    }
    
    // TODO give rewards to player
    public PlayerChangeInfo deleteBuild(long buildId, PlayerChangeInfo changes) {
        // Create change info
        if (changes == null) {
            changes = new PlayerChangeInfo();
        }
        
        // Get build
        var build = this.getBuilds().remove(buildId);
        
        if (build != null) {
            build.delete();
        }
        
        return changes;
    }
    
    // Database
    
    private void loadFromDatabase() {
        this.builds = new Long2ObjectOpenHashMap<>();
        
        Nebula.getGameDatabase().getObjects(StarTowerBuild.class, "playerUid", getPlayerUid()).forEach(build -> {
            this.builds.put(build.getUid(), build);
        });
    }
}
