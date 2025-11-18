package emu.nebula.game.tower;

import emu.nebula.Nebula;
import emu.nebula.data.GameData;
import emu.nebula.data.resources.StarTowerGrowthNodeDef;
import emu.nebula.game.player.Player;
import emu.nebula.game.player.PlayerChangeInfo;
import emu.nebula.game.player.PlayerManager;
import emu.nebula.game.player.PlayerProgress;
import emu.nebula.game.quest.QuestCondType;
import emu.nebula.proto.StarTowerApply.StarTowerApplyReq;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import lombok.Getter;

@Getter
public class StarTowerManager extends PlayerManager {
    // Tower game instance
    private StarTowerGame game;
    
    // Tower builds
    private Long2ObjectMap<StarTowerBuild> builds;
    private StarTowerBuild lastBuild;
    
    public StarTowerManager(Player player) {
        super(player);
    }
    
    public PlayerProgress getProgress() {
        return this.getPlayer().getProgress();
    }
    
    // Growth nodes (talents/research)
    
    public boolean hasGrowthNode(int id) {
        // Get growth node data
        var data = GameData.getStarTowerGrowthNodeDataTable().get(id);
        if (data == null) return false;
        
        // Check if bit is set
        return hasGrowthNode(data);
    }
    
    public boolean hasGrowthNode(StarTowerGrowthNodeDef data) {
        // Get current growth nodes
        var growth = getPlayer().getProgress().getStarTowerGrowth();
        
        // Get group index
        int groupIndex = data.getGroup() - 1;
        if (groupIndex >= growth.length) {
            return false;
        }
        
        // Get nodes bits
        int nodes = growth[groupIndex];
        int test = (1 << data.getNodeId());
        
        // Check if bit is set
        return (nodes & test) != 0;
    }
    
    public PlayerChangeInfo unlockGrowthNode(int id) {
        // Get growth node data
        var data = GameData.getStarTowerGrowthNodeDataTable().get(id);
        if (data == null) return null;
        
        // Make sure node is not already set
        if (this.hasGrowthNode(data)) {
            return null;
        }
        
        // Check if we have the required items to unlock
        if (!getPlayer().getInventory().hasItem(data.getItemId1(), data.getItemQty1())) {
            return null;
        }
        
        // Set node
        this.getProgress().setStarTowerGrowthNode(data.getGroup(), data.getNodeId());
        
        // Save to database
        Nebula.getGameDatabase().update(
            this.getProgress(),
            this.getPlayerUid(),
            "starTowerGrowth",
            this.getProgress().getStarTowerGrowth()
        );
        
        // Remove items
        return getPlayer().getInventory().removeItem(data.getItemId1(), data.getItemQty1());
    }
    
    public PlayerChangeInfo unlockGrowthNodeGroup(int group) {
        // Create variables
        var change = new PlayerChangeInfo();
        var unlocked = new IntArrayList();
        
        // Filter data
        for (var data : GameData.getStarTowerGrowthNodeDataTable()) {
            // Filter out nodes that are not from our group
            if (data.getGroup() != group) {
                continue;
            }
            
            // Filter out set growth nodes
            if (this.hasGrowthNode(data)) {
                continue;
            }
            
            // Check if we have the required items to unlock
            if (!getPlayer().getInventory().hasItem(data.getItemId1(), data.getItemQty1())) {
                continue;
            }
            
            // Set node
            this.getProgress().setStarTowerGrowthNode(data.getGroup(), data.getNodeId());
            
            // Remove items
            getPlayer().getInventory().removeItem(data.getItemId1(), data.getItemQty1());
            
            // Add to unlocked list
            unlocked.add(data.getId());
        }
        
        // Save to database if any nodes were unlocked
        if (unlocked.size() > 0) {
            // Save to database
            Nebula.getGameDatabase().update(
                this.getProgress(),
                this.getPlayerUid(),
                "starTowerGrowth",
                this.getProgress().getStarTowerGrowth()
            );
        }
        
        // Set unlocked list
        change.setExtraData(unlocked);
        
        // Return change
        return change;
    }
    
    // Builds
    
    public Long2ObjectMap<StarTowerBuild> getBuilds() {
        if (this.builds == null) {
            this.loadFromDatabase();
        }
        
        return builds;
    }
    
    public StarTowerBuild getBuildById(long id) {
        return this.getBuilds().get(id);
    }

    public boolean hasBuild(long id) {
        return this.getBuilds().containsKey(id);
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
        
        // Trigger quest
        this.getPlayer().triggerQuest(QuestCondType.TowerEnterFloor, 1);
        
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
    
    // Build
    
    private PlayerChangeInfo dismantleBuild(StarTowerBuild build, PlayerChangeInfo change) {
        // Calculate quanity of tickets from record score
        int count = (int) Math.floor(build.getScore() / 100);
        
        // Add journey tickets
        this.getPlayer().getInventory().addItem(12, count, change);
        
        // Success
        return change;
    }
    
    public PlayerChangeInfo saveBuild(boolean delete, String name, boolean lock) {
        // Sanity check
        if (this.getLastBuild() == null) {
            return null;
        }
        
        // Create player change info
        var change = new PlayerChangeInfo();
        
        // Cache build and clear reference
        var build = this.lastBuild;
        this.lastBuild = null;
        
        // Check if the player wants this build or not
        if (delete) {
            return this.dismantleBuild(build, change);
        }
        
        // Check limit
        if (this.getBuilds().size() >= 50) {
            return null;
        }
        
        // Add to builds
        this.getBuilds().put(build.getUid(), build);
        
        // Save build to database
        build.save();
        
        // Success
        return change;
    }
    
    public PlayerChangeInfo deleteBuild(long buildId, PlayerChangeInfo change) {
        // Create change info
        if (change == null) {
            change = new PlayerChangeInfo();
        }
        
        // Get build
        var build = this.getBuilds().remove(buildId);
        
        if (build == null) {
            return change;
        }
        
        // Delete
        build.delete();
        
        // Add journey tickets
        this.dismantleBuild(build, change);
        
        // Success
        return change;
    }
    
    // Database
    
    public void loadFromDatabase() {
        this.builds = new Long2ObjectOpenHashMap<>();
        
        Nebula.getGameDatabase().getObjects(StarTowerBuild.class, "playerUid", getPlayerUid()).forEach(build -> {
            this.builds.put(build.getUid(), build);
        });
    }
}
