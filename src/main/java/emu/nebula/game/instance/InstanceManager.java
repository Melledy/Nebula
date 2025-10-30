package emu.nebula.game.instance;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import emu.nebula.GameConstants;
import emu.nebula.Nebula;
import emu.nebula.data.GameData;
import emu.nebula.database.GameDatabaseObject;
import emu.nebula.game.player.Player;
import emu.nebula.game.player.PlayerChangeInfo;
import emu.nebula.game.player.PlayerManager;
import emu.nebula.proto.PlayerData.PlayerInfo;
import emu.nebula.proto.Public.CharGemInstance;
import emu.nebula.proto.Public.DailyInstance;
import emu.nebula.proto.Public.Energy;
import emu.nebula.proto.Public.RegionBossLevel;
import emu.nebula.proto.Public.SkillInstance;
import emu.nebula.proto.Public.WeekBossLevel;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import lombok.Getter;

@Getter
@Entity(value = "instances", useDiscriminator = false)
public class InstanceManager extends PlayerManager implements GameDatabaseObject {
    @Id
    private int uid;
    
    private Int2IntMap dailyInstanceLog;
    private Int2IntMap regionBossLog;
    private Int2IntMap skillInstanceLog;
    private Int2IntMap charGemLog;
    private Int2IntMap weekBossLog;
    
    private transient int curInstanceId;
    private transient int rewardType;
    
    @Deprecated // Morphia
    public InstanceManager() {
        
    }
    
    public InstanceManager(Player player) {
        super(player);
        this.uid = player.getUid();
        
        this.dailyInstanceLog = new Int2IntOpenHashMap();
        this.regionBossLog = new Int2IntOpenHashMap();
        this.skillInstanceLog = new Int2IntOpenHashMap();
        this.charGemLog = new Int2IntOpenHashMap();
        this.weekBossLog = new Int2IntOpenHashMap();
        
        this.save();
    }
    
    public void setCurInstanceId(int id) {
        this.setCurInstanceId(id, 0);
    }

    public void setCurInstanceId(int id, int rewardType) {
        this.curInstanceId = id;
        this.rewardType = rewardType;
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
    
    public PlayerChangeInfo settleInstance(InstanceData data, Int2IntMap log, String logName, int star) {
        // Calculate settle data
        var settleData = new InstanceSettleData();
        
        settleData.setWin(star > 0);
        settleData.setFirst(settleData.isWin() && !log.containsKey(data.getId()));
        
        // Init player changes
        var changes = new PlayerChangeInfo();
        
        // Handle win
        if (settleData.isWin()) {
            // Energy
            settleData.setExp(data.getEnergyConsume());
            getPlayer().consumeEnergy(settleData.getExp(), changes);
            
            // Awards
            getPlayer().getInventory().addItem(GameConstants.EXP_ITEM_ID, settleData.getExp(), changes);
            getPlayer().getInventory().addItems(data.getRewards(), changes);
            
            if (settleData.isFirst()) {
                getPlayer().getInventory().addItems(data.getFirstRewards(), changes);
            }
            
            // Log
            this.saveInstanceLog(log, logName, data.getId(), star);
        }
        
        // Log energy
        if (data.getEnergyConsume() > 0) {
            var energyProto = Energy.newInstance()
                    .setPrimary(getPlayer().getEnergy())
                    .setUpdateTime(Nebula.getCurrentTime() + 600);
            
            changes.add(energyProto);
        }
        
        // Set extra data
        changes.setExtraData(settleData);
        
        // Success
        return changes.setSuccess(true);
    }

    // Proto
    
    public void toProto(PlayerInfo proto) {
        // Init
        int minStars = 0;
        
        // Simple hack to unlock all instances
        if (Nebula.getConfig().getServerOptions().unlockInstances) {
            minStars = 1;
        }
        
        // Daily instance
        for (var data : GameData.getDailyInstanceDataTable()) {
            int stars = Math.max(getDailyInstanceLog().get(data.getId()), minStars);
            
            var p = DailyInstance.newInstance()
                    .setId(data.getId())
                    .setStar(stars);
            
            proto.addDailyInstances(p);
        }
        
        // Regional boss
        for (var data : GameData.getRegionBossLevelDataTable()) {
            int stars = Math.max(getRegionBossLog().get(data.getId()), minStars);
            
            var p = RegionBossLevel.newInstance()
                    .setId(data.getId())
                    .setStar(stars);
            
            proto.addRegionBossLevels(p);
        }
        
        // Skill instance
        for (var data : GameData.getSkillInstanceDataTable()) {
            int stars = Math.max(getSkillInstanceLog().get(data.getId()), minStars);
            
            var p = SkillInstance.newInstance()
                    .setId(data.getId())
                    .setStar(stars);
            
            proto.addSkillInstances(p);
        }
        
        // Char gem instance
        for (var data : GameData.getCharGemInstanceDataTable()) {
            int stars = Math.max(getCharGemLog().get(data.getId()), minStars);
            
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
    }
}
