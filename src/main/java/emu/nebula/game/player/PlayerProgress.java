package emu.nebula.game.player;

import java.util.HashMap;
import java.util.Map;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.PostLoad;
import emu.nebula.Nebula;
import emu.nebula.data.GameData;
import emu.nebula.data.resources.InfinityTowerLevelDef;
import emu.nebula.database.GameDatabaseObject;
import emu.nebula.game.tutorial.TutorialLevelLog;
import emu.nebula.game.vampire.VampireSurvivorLog;
import emu.nebula.proto.PlayerData.PlayerInfo;
import emu.nebula.proto.Public.CharGemInstance;
import emu.nebula.proto.Public.DailyInstance;
import emu.nebula.proto.Public.RegionBossLevel;
import emu.nebula.proto.Public.SkillInstance;
import emu.nebula.proto.Public.VampireSurvivorLevel;
import emu.nebula.proto.Public.WeekBossLevel;
import emu.nebula.util.Bitset;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import lombok.Getter;

@Getter
@Entity(value = "progress", useDiscriminator = false)
public class PlayerProgress extends PlayerManager implements GameDatabaseObject {
    @Id
    private int uid;
    
    // Star Tower
    private IntSet starTowerLog;
    private int[] starTowerGrowth;
    private int towerTickets;
    
    // Instances
    private Int2IntMap dailyInstanceLog;
    private Int2IntMap regionBossLog;
    private Int2IntMap skillInstanceLog;
    private Int2IntMap charGemLog;
    private Int2IntMap weekBossLog;
    
    // Infinite Arena
    private Int2IntMap infinityTowerLog;
    @Deprecated private Int2IntMap infinityArenaLog;
    
    // Vampire Survivors
    private Map<Integer, VampireSurvivorLog> vampireLog;
    private Bitset vampireTalents;
    
    // Fate cards
    private IntSet fateCards;
    
    // Tutorial
    private Map<Integer, TutorialLevelLog> tutorialLog;

    @Deprecated // Morphia only
    public PlayerProgress() {
        
    }
    
    public PlayerProgress(Player player) {
        super(player);
        this.uid = player.getUid();
        
        // Star Tower
        this.starTowerLog = new IntOpenHashSet();
        this.starTowerGrowth = new int[3];
        
        // Instances
        this.dailyInstanceLog = new Int2IntOpenHashMap();
        this.regionBossLog = new Int2IntOpenHashMap();
        this.skillInstanceLog = new Int2IntOpenHashMap();
        this.charGemLog = new Int2IntOpenHashMap();
        this.weekBossLog = new Int2IntOpenHashMap();
        
        // Infinity Tower
        this.infinityTowerLog = new Int2IntOpenHashMap();
        
        // Vampire Survivor
        this.vampireLog = new HashMap<>();
        this.vampireTalents = new Bitset();
        
        // Fate cards
        this.fateCards = new IntOpenHashSet();
        
        // Tutorials
        this.tutorialLog = new HashMap<>();
        
        // Save to database
        this.save();
    }
    
    public void addStarTowerLog(int id) {
        // Sanity check
        if (this.getStarTowerLog().contains(id)) {
            return;
        }
        
        // Add & Save to database
        this.getStarTowerLog().add(id);
        Nebula.getGameDatabase().addToSet(this, this.getUid(), "starTowerLog", id);
    }
    
    public boolean setStarTowerGrowthNode(int group, int nodeId) {
        // Get index
        int index = group - 1;
        if (index < 0) return false;
        
        // Grow growth if its too small
        if (index >= this.starTowerGrowth.length) {
            var old = this.starTowerGrowth;
            this.starTowerGrowth = new int[index + 1];
            System.arraycopy(old, 0, this.starTowerGrowth, 0, old.length);
        }
        
        // Set
        this.starTowerGrowth[index] |= (1 << (nodeId - 1));
        
        // Success
        return true;
    }
    
    /**
     * Returns the maximum amount of weekly tickets that a player can receive without hitting the limit
     */
    public int getMaxEarnableWeeklyTowerTickets() {
        return Math.max(this.getWeeklyTowerTicketLimit() - this.getTowerTickets(), 0);
    }
    
    public int getWeeklyTowerTicketLimit() {
        int limit = 2000;
        
        if (this.getPlayer().getStarTowerManager().hasGrowthNode(10502)) {
            limit += 1000;
        } else if (this.getPlayer().getStarTowerManager().hasGrowthNode(10201)) {
            limit += 500;
        }
        
        return limit;
    }

    public void addWeeklyTowerTicketLog(int count) {
        this.towerTickets += count;
        Nebula.getGameDatabase().update(this, this.getUid(), "towerTickets", this.towerTickets);
    }
    
    public void clearWeeklyTowerTicketLog() {
        if (this.towerTickets == 0) {
            return;
        }
        
        this.towerTickets = 0;
        Nebula.getGameDatabase().update(this, this.getUid(), "towerTickets", this.towerTickets);
    }
    
    public void addInfinityTowerLog(InfinityTowerLevelDef level) {
        // Calculate tower id
        int towerId = level.getTowerId();
        int levelId = level.getId();
        
        if (towerId <= 0) {
            return;
        }
        
        // Check highest clear
        int highestClearId = this.getInfinityTowerLog().get(towerId);

        // Add & Save to database
        if (levelId > highestClearId) {
            this.getInfinityTowerLog().put(towerId, levelId);
            Nebula.getGameDatabase().update(this, this.getUid(), "infinityArenaLog." + towerId, levelId);
        }
    }
    
    public void saveInstanceLog(Int2IntMap log, String logName, int id, int newStar) {
        // Get current star
        int star = log.get(id);
        
        // Check star
        if (newStar <= star || newStar > 7) {
            return;
        }
        
        // Add to log and update database
        log.put(id, newStar);
        Nebula.getGameDatabase().update(this, this.getUid(), logName + "." + id, newStar);
    }
    
    // Proto
    
    public void encodePlayerInfo(PlayerInfo proto) {
        // Check if we want to unlock all instances
        boolean unlockAll = Nebula.getConfig().getServerOptions().unlockInstances;
        
        // Star tower
        if (unlockAll) {
            // Force unlock all monoliths
            for (var towerData : GameData.getStarTowerDataTable()) {
                proto.addRglPassedIds(towerData.getId());
            }
        } else {
            for (var towerId : this.getStarTowerLog()) {
                proto.addRglPassedIds(towerId);
            }
        }
        
        // Simple hack to unlock all instances
        int minStars = unlockAll ? 1 : 0;
        
        // Daily instance
        for (var data : GameData.getDailyInstanceDataTable()) {
            int stars = Math.max(this.getDailyInstanceLog().get(data.getId()), minStars);
            
            var p = DailyInstance.newInstance()
                    .setId(data.getId())
                    .setStar(stars);
            
            proto.addDailyInstances(p);
        }
        
        // Regional boss
        for (var data : GameData.getRegionBossLevelDataTable()) {
            int stars = Math.max(this.getRegionBossLog().get(data.getId()), minStars);
            
            var p = RegionBossLevel.newInstance()
                    .setId(data.getId())
                    .setStar(stars);
            
            proto.addRegionBossLevels(p);
        }
        
        // Skill instance
        for (var data : GameData.getSkillInstanceDataTable()) {
            int stars = Math.max(this.getSkillInstanceLog().get(data.getId()), minStars);
            
            var p = SkillInstance.newInstance()
                    .setId(data.getId())
                    .setStar(stars);
            
            proto.addSkillInstances(p);
        }
        
        // Char gem instance
        for (var data : GameData.getCharGemInstanceDataTable()) {
            int stars = Math.max(this.getCharGemLog().get(data.getId()), minStars);
            
            var p = CharGemInstance.newInstance()
                    .setId(data.getId())
                    .setStar(stars);
            
            proto.addCharGemInstances(p);
        }
        
        // Weekly boss
        for (var data : GameData.getWeekBossLevelDataTable()) {
            var p = WeekBossLevel.newInstance()
                    .setId(data.getId())
                    .setFirst(this.getWeekBossLog().get(data.getId()) == 1);
            
            proto.addWeekBossLevels(p);
        }
        
        // Vampire survivors
        var vsProto = proto.getMutableVampireSurvivorRecord();
        vsProto.getMutableSeason();
        
        if (unlockAll) {
            // Force unlock all vampire survivor records if we dont have the records
            for (var vsData : GameData.getVampireSurvivorDataTable()) {
                // Get existing record
                var log = this.getVampireLog().get(vsData.getId());
                
                if (log == null) {
                    var level = VampireSurvivorLevel.newInstance()
                            .setId(vsData.getId())
                            .setScore(0)
                            .setPassed(true);
                    
                    vsProto.addRecords(level);
                } else {
                    vsProto.addRecords(log.toProto());
                }
            }
        } else {
            for (var log : this.getVampireLog().values()) {
                vsProto.addRecords(log.toProto());
            }
        }
        
        // Tutorials
        for (var tutorial : this.getTutorialLog().values()) {
            proto.addTutorialLevels(tutorial.toProto());
        }
    }
    
    // Database fix
    
    @PostLoad
    public void postLoad() {
        boolean shouldSave = false;
        
        // Fix missing star tower growth
        if (this.starTowerGrowth == null) {
            this.starTowerGrowth = new int[1];
            shouldSave = true;
        }
        
        // Fix missing infinity tower log
        if (this.infinityTowerLog == null) {
            this.infinityTowerLog = new Int2IntOpenHashMap();
            shouldSave = true;
        }
        
        // Carry over infinity tower progress
        if (this.infinityArenaLog != null) {
            for (int levelId : this.infinityArenaLog.values()) {
                var data = GameData.getInfinityTowerLevelDataTable().get(levelId);
                if (data == null) {
                    continue;
                }
                
                int towerId = data.getTowerId();
                
                if (towerId > 0) {
                    this.infinityTowerLog.put(data.getTowerId(), levelId);
                }
            }
            
            // Clear old infinity tower logs when done
            this.infinityArenaLog = null;
            shouldSave = true;
        }
        
        // Update in database if anything changed
        if (shouldSave) {
            this.save();
        }
    }
}
