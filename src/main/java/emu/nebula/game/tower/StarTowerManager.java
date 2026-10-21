package emu.nebula.game.tower;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import emu.nebula.GameConstants;
import emu.nebula.Nebula;
import emu.nebula.data.GameData;
import emu.nebula.data.resources.StarTowerGrowthNodeDef;
import emu.nebula.game.achievement.AchievementCondition;
import emu.nebula.game.character.GameCharacter;
import emu.nebula.game.player.Player;
import emu.nebula.game.player.PlayerChangeInfo;
import emu.nebula.game.player.PlayerManager;
import emu.nebula.game.player.PlayerProgress;
import emu.nebula.game.quest.QuestCondition;
import emu.nebula.proto.Public.TowerVitaProject;
import emu.nebula.proto.PublicStarTower.StarTowerBookCharPotential;
import emu.nebula.proto.StarTowerApply.StarTowerApplyReq;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import lombok.Getter;

@Getter
public class StarTowerManager extends PlayerManager {
    // Star tower game instance
    private StarTowerGame game;
    
    // Star tower records
    private Long2ObjectMap<StarTowerBuild> builds;
    private StarTowerBuild lastBuild;
    
    // Potential presets
    private Long2ObjectMap<StarTowerPotentialPreset> presets;
    
    public StarTowerManager(Player player) {
        super(player);
    }
    
    public PlayerProgress getProgress() {
        return this.getPlayer().getProgress();
    }
    
    public IntSet getStarTowerLog() {
        return this.getProgress().getStarTowerLog();
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
        int test = (1 << (data.getNodeId() - 1));
        
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
        
        // Star Tower quests
        this.getPlayer().trigger(QuestCondition.TowerGrowthSpecificNote, 1, data.getId());
        this.getPlayer().trigger(QuestCondition.TowerGrowthWithSpecificNote, 1, data.getId());
        
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
            getPlayer().getInventory().removeItem(data.getItemId1(), data.getItemQty1(), change);
            
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
            
            // Star Tower quests
            for (int i = 0; i < unlocked.size(); i++) {
                int nodeId = unlocked.getInt(i);
                
                this.getPlayer().trigger(QuestCondition.TowerGrowthSpecificNote, 1, nodeId);
                this.getPlayer().trigger(QuestCondition.TowerGrowthWithSpecificNote, 1, nodeId);
            }
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
        
        return this.builds;
    }
    
    public StarTowerBuild getBuildById(long id) {
        return this.getBuilds().get(id);
    }

    public boolean hasBuild(long id) {
        return this.getBuilds().containsKey(id);
    }
    
    public void addBuild(StarTowerBuild build) {
        // Add to builds
        this.getBuilds().put(build.getUid(), build);
        
        // Save build to database
        build.save();
        
        // Achievement
        var rank = build.getRank();
        if (rank != null) {
            this.getPlayer().getAchievementManager().trigger(
                AchievementCondition.TowerBuildSpecificScoreWithTotal,
                1,
                rank.getRarity(),
                0
            );
            
            // Star Tower quests use the record's rating level, not its rarity.
            // NOTE: This must stay on the quest manager directly. Condition 504 is also
            // used by the build-rarity achievements (param1 = rarity), so routing this
            // through Player::trigger would feed them the level instead of the rarity.
            this.getPlayer().getQuestManager().trigger(
                QuestCondition.TowerBuildSpecificScoreWithTotal.getValue(),
                1,
                rank.getLevel(),
                0
            );
        }
    }
    
    public PlayerChangeInfo saveBuild(boolean delete, String name, boolean lock) {
        // Sanity check
        if (this.getLastBuild() == null) {
            return null;
        }
        
        // Create player change info
        var change = new PlayerChangeInfo();
        
        // Cache build
        var build = this.lastBuild;
        
        // Clear reference to build
        this.lastBuild = null;
        
        // Check if the player wants this build or not
        if (delete) {
            return this.dismantleBuild(build, change);
        }
        
        // Check limit
        if (this.getBuilds().size() >= GameConstants.MAX_BUILDS) {
            return null;
        }
        
        // Update seen potentials
        this.updateSeenPotentials(build);
        
        // Add build for player
        this.addBuild(build);
        
        // Success
        return change;
    }
    
    private PlayerChangeInfo dismantleBuild(StarTowerBuild build, PlayerChangeInfo change) {
        // Calculate quanity of tickets from record score
        int count = (int) Math.floor(build.getScore() / 100);
        
        // Check weekly tickets
        int maxAmount = this.getPlayer().getProgress().getMaxEarnableWeeklyTowerTickets();
        count = Math.min(maxAmount, count);
        
        // Add journey tickets
        this.getPlayer().getInventory().addItem(12, count, change);
        
        // Add to weekly ticket log
        this.getPlayer().getProgress().addWeeklyTowerTicketLog(count);
        
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
    
    // Seen potentials
    
    public void updateSeenPotentials(StarTowerBuild build) {
        var updateChars = new HashSet<GameCharacter>();
        
        for (var entry : build.getPotentials().int2IntEntrySet()) {
            int potentialId = entry.getIntKey();
            int level = entry.getIntValue();
            
            var character = this.getPlayer().getCharacters().getCharacterFromPotential(potentialId);
            if (character == null) continue;
            
            boolean success = character.setSeenPotential(potentialId, level);
            
            if (success) {
                updateChars.add(character);
            }
        }
        
        if (updateChars.size() > 0) {
            for (var character : updateChars) {
                character.save();
            }
        }
    }
    
    // Presets
    
    public Long2ObjectMap<StarTowerPotentialPreset> getPresets() {
        if (this.presets == null) {
            this.loadFromDatabase();
        }
        
        return this.presets;
    }
    
    public StarTowerPotentialPreset getPresetById(long presetId) {
        return this.getPresets().get(presetId);
    }
    
    public StarTowerPotentialPreset importPreset(String name, boolean preference, Iterable<StarTowerBookCharPotential> potentials) {
        // Check max presets limit
        if (this.getPresets().size() >= GameConstants.MAX_PRESETS) {
            return null;
        }
        
        // Check name
        if (name == null) {
            name = "";
        }
        
        // Clamp name if it's too long
        if (name.length() > 32) {
            name = name.substring(0, 31);
        }
        
        // Create preset
        var preset = new StarTowerPotentialPreset(this.getPlayer(), name, preference, potentials);
        
        // Save to database
        preset.save();
        
        // Add to map
        this.getPresets().put(preset.getUid(), preset);
        
        // Return preset
        return preset;
    }
    
    public boolean deletePreset(StarTowerPotentialPreset preset) {
        // Get preset
        var removed = this.getPresets().remove(preset.getUid());
        if (removed == null) {
            return false;
        }
        
        // Delete from database
        removed.delete();
        
        // Success
        return true;
    }
    
    // Vita projects (auto decision plans)
    
    public StarTowerVitaData getVitaData() {
        return this.getProgress().getTowerVitaData();
    }
    
    public List<StarTowerVitaProject> getVitaProjects() {
        return this.getVitaData().getProjects().values().stream()
                .sorted(Comparator.comparingInt(StarTowerVitaProject::getId))
                .toList();
    }
    
    public StarTowerVitaProject getVitaProject(int id) {
        return this.getVitaData().getProject(id);
    }
    
    public StarTowerVitaProject updateVitaProject(TowerVitaProject proto) {
        // Sanity check
        if (proto == null || proto.getId() < 1 || proto.getId() > GameConstants.MAX_VITA_PROJECTS) {
            return null;
        }
        
        // Get or create the project
        var data = this.getVitaData();
        var project = data.getProject(proto.getId());
        
        if (project == null) {
            project = new StarTowerVitaProject(proto.getId());
        }
        
        // Update name
        if (proto.hasName()) {
            project.setName(proto.getName());
        }
        
        // The client only sends options that differ from the defaults, so replace the whole list
        project.getOptions().clear();
        
        for (var optionProto : proto.getOptions()) {
            var option = StarTowerVitaOption.fromProto(optionProto);
            
            if (option != null) {
                project.putOption(option);
            }
        }
        
        // Save project to database
        data.putProject(project);
        
        // Success
        return project;
    }
    
    public StarTowerVitaProject renameVitaProject(int id, String name) {
        // Sanity check
        if (id < 1 || id > GameConstants.MAX_VITA_PROJECTS) {
            return null;
        }
        
        // Get or create the project
        var data = this.getVitaData();
        var project = data.getProject(id);
        
        if (project == null) {
            project = new StarTowerVitaProject(id);
        }
        
        // Update name
        project.setName(name);
        
        // Save project to database
        data.putProject(project);
        
        // Success
        return project;
    }
    
    public boolean selectVitaProject(int id) {
        // Sanity check
        if (id < 1 || id > GameConstants.MAX_VITA_PROJECTS) {
            return false;
        }
        
        // Save selection to database
        this.getVitaData().setSelectedProjectId(id);
        
        // Success
        return true;
    }
    
    // Game
    
    public PlayerChangeInfo apply(StarTowerApplyReq req) {
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
        
        // Create change info
        var change = new PlayerChangeInfo();
        int expressItemId = 0;
        
        // Check if sweeping
        if (req.getSweep()) {
            // Key to the Stairs to the Stars
            // Make sure we have the proper growth node that enables the express pass
            if (!this.hasGrowthNode(10301)) {
                return null;
            }
            
            // Check if we have this tower completed
            boolean unlockInstances = Nebula.getConfig().getServerOptions().isUnlockInstances();
            if (!unlockInstances && !getPlayer().getProgress().getStarTowerLog().contains(data.getId())) {
                return null;
            }
            
            // Check if the player has any express pass
            if (this.getPlayer().getInventory().hasItem(29, 1)) {
                expressItemId = 29;
            } else if (this.getPlayer().getInventory().hasItem(30, 1)) {
                expressItemId = 30;
            } else {
                return null;
            }
        }
        
        // Create game
        try {
            this.game = new StarTowerGame(this, data, formation, req);
        } catch (Exception e) {
            Nebula.getLogger().error("Could not create star tower game", e);
            return null;
        }
        
        // Consume express pass item
        if (expressItemId > 0) {
            this.getPlayer().getInventory().removeItem(expressItemId, 1, change);
        }
        
        // Trigger quest
        this.getPlayer().trigger(QuestCondition.TowerEnterFloor, 1);
        this.getPlayer().trigger(QuestCondition.TowerEnterTotal, 1);
        
        // Success
        return change.setExtraData(this.game);
    }

    public StarTowerGame settleGame(boolean victory) {
        // Cache instance
        var game = this.game;
        
        if (game == null) {
            return null;
        }
        
        // Clear game
        this.game = null;
        
        // Set last build
        this.lastBuild = game.getBuild();
        
        // Handle victory events
        if (victory) {
            // Add star tower history
            this.getPlayer().getProgress().addStarTowerLog(game.getId());
            
            // Star Tower quests (group + difficulty)
            // NOTE: This must stay on the quest manager directly. Condition 506 is also
            // used by the "Monolith Conqueror" achievements, but they interpret the
            // params as (difficulty, 0) instead of (groupId, difficulty), so routing
            // this through Player::trigger would corrupt them.
            this.getPlayer().getQuestManager().trigger(
                QuestCondition.TowerClearSpecificGroupIdAndDifficulty.getValue(),
                1,
                game.getData().getGroupId(),
                game.getDifficulty()
            );
            
            // Achievement conditions
            var achievements = this.getPlayer().getAchievementManager();
            
            achievements.trigger(AchievementCondition.TowerClearTotal, 1);
            achievements.trigger(
                AchievementCondition.TowerClearSpecificLevelWithDifficultyAndTotal,
                1,
                game.getData().getId(),
                0
            );
            
            var elementType = game.getTeamElement();
            if (elementType != null) {
                achievements.trigger(AchievementCondition.TowerClearSpecificCharacterTypeWithTotal, 1, elementType.getValue(), 0);
            }
            
            // Update tower group achievements
            this.updateTowerGroupAchievements(game);
        }
        
        // Return game
        return game;
    }
    
    // Achievements
    
    private void updateTowerGroupAchievements(StarTowerGame game) {
        // Update "First Ascension" achievement
        boolean firstAscension = this.getStarTowerLog().contains(401) && this.getStarTowerLog().size() >= 2;
        if (firstAscension) {
            this.getPlayer().getAchievementManager().triggerOne(498, 1, 0, 1);
        }
        
        // Get total clears on this difficulty
        int diff = game.getDifficulty();
        int totalDiffClears = 0;
        
        for (int i = 1; i <= 3; i++) {
            int towerId = (i * 100) + 1 + diff;
            if (this.getStarTowerLog().contains(towerId)) {
                totalDiffClears++;
            }
        }
        
        // Update "Monolith Conqueror" achievements
        this.getPlayer().getAchievementManager().trigger(
            AchievementCondition.TowerClearSpecificGroupIdAndDifficulty,
            totalDiffClears,
            diff,
            0
        );
    }
    
    // Database
    
    public void loadFromDatabase() {
        // Init variables
        this.builds = new Long2ObjectOpenHashMap<>();
        this.presets = new Long2ObjectOpenHashMap<>();
        
        // Load builds with the current player's uid
        Nebula.getGameDatabase().getObjects(StarTowerBuild.class, "playerUid", getPlayerUid()).forEach(build -> {
            // Fix outdated builds
            if (build.getSecondarySkills() == null) {
                build.calculateScore();
                build.save();
            }
            
            // Add build
            this.builds.put(build.getUid(), build);
        });
        
        // Load presets with the current player's uid
        Nebula.getGameDatabase().getObjects(StarTowerPotentialPreset.class, "playerUid", getPlayerUid()).forEach(preset -> {
            // Add preset
            this.presets.put(preset.getUid(), preset);
        });
        
        // Migration: Check if we need to update any seen potentials
        boolean needsPotentialMigration = this.builds.size() > 0 && this.getPlayer().getCharacters().getCharacterCollection()
                .stream()
                .allMatch(c -> c.getSeenPotentials().isEmpty());
        
        if (needsPotentialMigration) {
            for (var build : this.getBuilds().values()) {
                this.updateSeenPotentials(build);
            }
        }
    }
}
