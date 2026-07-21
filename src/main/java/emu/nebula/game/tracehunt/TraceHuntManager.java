package emu.nebula.game.tracehunt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import emu.nebula.GameConstants;
import emu.nebula.Nebula;
import emu.nebula.data.GameData;
import emu.nebula.data.resources.TraceHuntControlDef;
import emu.nebula.data.resources.TraceHuntSelfHuntExtraCostDef;
import emu.nebula.database.GameDatabaseObject;
import emu.nebula.game.player.Player;
import emu.nebula.game.player.PlayerChangeInfo;
import emu.nebula.game.player.PlayerManager;
import emu.nebula.proto.TraceHuntInfoOuterClass.TraceHuntInfo;
import emu.nebula.util.Utils;
import lombok.Getter;

@Getter
@Entity(value = "trace_hunts", useDiscriminator = false)
public class TraceHuntManager extends PlayerManager implements GameDatabaseObject {
    @Id
    private int uid;
    
    private int controlId;
    private int level;
    private int exp;
    
    // Requests/Hunt items
    private int traceRequests;
    private int dailyRequests; // Max of 20 daily
    
    private int huntPermits;
    
    // Current boss
    private int bossId;
    private long bossCreateTime;
    private int traceProgress;
    private int huntProgress;
    
    private int huntCount;
    private int assistCount;
    
    private List<TraceHuntLog> traceLogs;
    private List<TraceHuntLog> huntLogs;
    
    // Current hunt
    private transient int huntPlayerUid;
    private transient int huntBossId;
    private long buildId;
    
    // Collection
    private Map<Integer, TraceHuntBoss> bosses;
    
    @Deprecated // Morphia only
    public TraceHuntManager() {
        
    }
    
    public TraceHuntManager(Player player) {
        this();
        this.setPlayer(player);
        this.uid = player.getUid();
        
        this.controlId = 3;
        this.level = 1;

        this.bossId = this.getControlData().getBossList()[0];
        this.bossCreateTime = Nebula.getCurrentServerTime();
        this.traceLogs = new ArrayList<>();
        this.huntLogs = new ArrayList<>();
        
        this.bosses = new HashMap<>();
    }
    
    public int getControlId() {
        return this.controlId;
    }
    
    public TraceHuntControlDef getControlData() {
        return GameData.getTraceHuntControlDataTable().get(this.getControlId());
    }
    
    public int addTraceRequests(int amount) {
        // Add/remove requests
        this.traceRequests = Math.max(Math.min(this.traceRequests + amount, 60), 0);
        
        // Save to database
        this.save();
        
        // Return amount
        return this.traceRequests;
    }

    public int addHuntPermits(int amount) {
        // Add/remove permits
        this.huntPermits = Math.max(this.huntPermits + amount, 0);

        // Save to database
        this.save();
        
        // Return amount
        return this.huntPermits;
    }
    
    public int getMaxGainableExp() {
        if (this.getLevel() >= this.getMaxLevel()) {
            return 0;
        }
        
        int maxLevel = this.getMaxLevel();
        int max = 0;
        
        for (int i = this.getLevel() + 1; i <= maxLevel; i++) {
            var data = GameData.getTraceHuntLevelDataTable().get(i);
            
            if (data != null) {
                max += data.getExp();
            }
        }
        
        return Math.max(max - this.getExp(), 0);
    }
    
    public int getMaxExp() {
        if (this.getLevel() >= this.getMaxLevel()) {
            return 0;
        }
        
        var data = GameData.getTraceHuntLevelDataTable().get(this.level + 1);
        return data != null ? data.getExp() : 0;
    }
    
    public int getMaxLevel() {
        return GameData.getTraceHuntLevelDataTable().size();
    }
    
    public void addExp(int amount) {
        // Setup
        int expRequired = this.getMaxExp();

        // Add exp
        this.exp += amount;

        // Check for level ups
        while (this.exp >= expRequired && expRequired > 0) {
            this.level += 1;
            this.exp -= expRequired;
            
            expRequired = this.getMaxExp();
        }
        
        // Clamp exp
        if (this.getLevel() >= this.getMaxLevel()) {
            this.exp = 0;
        }
        
        // Save to database
        this.save();
    }
    
    public boolean isHunting() {
        return this.traceProgress >= GameConstants.TRACE_HUNT_MAX_TRACE_PROGRESS;
    }
    
    public boolean isHuntComplete() {
        return this.huntProgress >= GameConstants.TRACE_HUNT_MAX_HUNT_PROGRESS;
    }
    
    public TraceHuntSelfHuntExtraCostDef getHuntCost() {
        int times = Math.max(this.huntCount, GameData.getTraceHuntSelfHuntExtraCostDataTable().size());
        return GameData.getTraceHuntSelfHuntExtraCostDataTable().get(times);
    }
    
    public void resetBoss() {
        this.bossId = 0;
        this.bossCreateTime = 0;
        this.traceProgress = 0;
        this.huntProgress = 0;
        
        this.huntCount = 0;
        this.assistCount = 0;
        
        this.traceLogs.clear();
        this.huntLogs.clear();
    }
    
    // Game logic
    
    public PlayerChangeInfo trace(int count) {
        // Sanity check to make sure we have enough requests
        if (this.getTraceRequests() < count) {
            return null;
        }
        
        // Sanity check to make sure we havent started hunting yet
        if (this.isHunting()) {
            return null;
        }
        
        // Used requests
        int consumed = 0;
        
        // Parse log
        var logs = new ArrayList<TraceHuntLog>();
        
        for (int i = 0; i < count; i++) {
            // Get random trace
            int traceId = Utils.randomElement(GameConstants.TRACE_HUNT_TRACE_IDS);
            int progress = 0;
            
            if (traceId >= 40) {
                progress = 2000;
            } else if (traceId >= 30) {
                progress = 1000;
            } else {
                progress = 500;
            }
            
            // Add trace log
            logs.add(new TraceHuntLog(traceId, Integer.toString(progress)));
            
            // Add progress
            this.traceProgress += progress;
            
            // Increment requests we used
            consumed++;
            
            // Complete
            if (this.isHunting()) {
                logs.add(new TraceHuntLog(5));
                break;
            }
        }
        
        // Consume trace requests
        var change = this.getPlayer().getInventory().removeItem(GameConstants.TRACE_HUNT_REQUEST_ITEM_ID, consumed);
        
        // Add to logs
        this.getTraceLogs().addAll(logs);
        
        // Add logs to change info
        change.setExtraData(logs);
        
        // Save
        this.save();
        
        // Success
        return change;
    }
    
    public boolean apply(long ownerUid, int bossId, long buildId) {
        // Sanity check
        if (!this.isHunting() || this.isHuntComplete()) {
            return false;
        }
        
        // Set build
        this.huntPlayerUid = (int) ownerUid;
        this.huntBossId = bossId;
        this.buildId = buildId;
        
        // Success
        return true;
    }

    public PlayerChangeInfo settle(int score, int stars) {
        // Sanity check
        if (this.huntBossId == 0 || this.huntPlayerUid == 0) {
            return null;
        }
        
        // TODO verify stars
        stars = Math.min(Math.max(stars, 0), 7);
        
        // Build change info
        var change = new PlayerChangeInfo();
        var logs = new ArrayList<TraceHuntLog>();
        
        // Add logs to change info
        change.setExtraData(logs);
        
        // Calculate result
        if (this.huntPlayerUid == this.getPlayerUid()) {
            // Check if we have enough cost items
            var cost = this.getHuntCost();
            
            if (cost != null) {
                if (!getPlayer().getInventory().hasItem(cost.getExtraCost1Tid(), cost.getExtraCost1Qty())) {
                    return null;
                }
                
                getPlayer().getInventory().removeItem(cost.getExtraCost1Tid(), cost.getExtraCost1Qty(), change);
            }
            
            // Our own hunts
            int progress = stars * 500;
            
            logs.add(new TraceHuntLog(11, this.getPlayer().getName(), Integer.toString(progress)));
            
            // Add to logs
            this.huntProgress = Math.min(this.huntProgress + progress, GameConstants.TRACE_HUNT_MAX_HUNT_PROGRESS);
            this.huntCount++;
            this.getHuntLogs().addAll(logs);
            
            // Save
            this.save();
        } else {
            // Other player's hunts are not supported yet TODO
            return null;
        }
        
        // Success
        return change;
    }

    public PlayerChangeInfo claimReward() {
        if (!this.isHuntComplete()) {
            return null;
        }
        
        // Add boss log
        var bossLog = new TraceHuntBoss(this.getBossId(), this.getHuntCount(), this.getAssistCount());
        this.getBosses().put(this.getBossId(), bossLog);
        
        // Reset boss
        this.resetBoss();
        
        // Get next boss from control data
        var control = this.getControlData();
        
        for (int bossId : control.getBossList()) {
            if (this.getBosses().containsKey(bossId)) {
                continue;
            }
            
            this.bossId = bossId;
            break;
        }
        
        // Save
        this.save();
        
        // Success
        // Test amount of items
        return this.getPlayer().getInventory().addItem(37, 1000);
    }
    
    // Proto
    
    public TraceHuntInfo toProto() {
        var proto = TraceHuntInfo.newInstance()
                .setControlID(this.getControlId())
                .setLevel(this.getLevel())
                .setExp(this.getExp())
                .setBossID(this.getBossId())
                .setBossCreateTime(this.getBossCreateTime())
                .setBuildID(this.getBuildId())
                .setTraceProgress(this.getTraceProgress())
                .setHuntProgress(this.getHuntProgress())
                .setSelfHuntTimes(this.getHuntCount());
        
        for (var boss : this.getBosses().values()) {
            proto.addBossCollections(boss.toProto());
        }
        
        for (var log : this.getTraceLogs()) {
            proto.addTraceLog(log.toProto());
        }
        
        for (var log : this.getHuntLogs()) {
            proto.addHuntLog(log.toProto());
        }
        
        return proto;
    }
}
