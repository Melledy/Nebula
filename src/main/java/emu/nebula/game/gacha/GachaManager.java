package emu.nebula.game.gacha;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import emu.nebula.Nebula;
import emu.nebula.data.GameData;
import emu.nebula.data.resources.GachaDef;
import emu.nebula.database.GameDatabaseObject;
import emu.nebula.game.player.Player;
import emu.nebula.game.player.PlayerChangeInfo;
import emu.nebula.game.player.PlayerManager;

import lombok.Getter;

@Getter
@Entity(value = "gacha", useDiscriminator = false)
public class GachaManager extends PlayerManager implements GameDatabaseObject {
    @Id
    private int uid;
    
    private Map<Integer, GachaBannerInfo> banners;
    private Map<Integer, List<GachaHistoryLog>> histories;
    
    @Deprecated // Morphia only
    public GachaManager() {
        
    }
    
    public GachaManager(Player player) {
        this();
        this.setPlayer(player);
        this.uid = player.getUid();
        
        this.banners = new HashMap<>();
        this.histories = new HashMap<>();
        
        this.save();
    }
    
    public synchronized Collection<GachaBannerInfo> getBannerInfos() {
        return this.banners.values();
    }
    
    public synchronized GachaBannerInfo getBannerInfo(GachaDef gachaData) {
        return this.banners.computeIfAbsent(
            gachaData.getId(), 
            i -> new GachaBannerInfo(gachaData)
        );
    }
    
    public void saveBanner(GachaBannerInfo info) {
        Nebula.getGameDatabase().update(
            this,
            this.getPlayerUid(),
            "banners." + info.getId(),
            info
        );
    }
    
    public PlayerChangeInfo recvGuarantee(int id) {
        // Get banner info
        var info = this.banners.get(id);
        if (info == null) {
            return null;
        }
        
        // Get banner data
        var data = GameData.getGachaDataTable().get(id);
        if (data == null) {
            return null;
        }
        
        // Check if we have enough pulls for a guarantee
        if (!data.canGuarantee() || info.getTotal() < data.getGuaranteeTimes()) {
            return null;
        }
        
        // Make sure we havent used our guarantee yet
        if (info.isUsedGuarantee()) {
            return null;
        }
        
        // Set guarantee
        info.setUsedGuarantee(true);

        // Update to database
        this.saveBanner(info);
        
        // Give player the guaranteed item
        return getPlayer().getInventory().addItem(data.getGuaranteeTid(), data.getGuaranteeQty());
    }
    
    // Histories
    
    public void addGachaHistory(GachaHistoryLog log) {
        // Get history
        var list = this.histories.computeIfAbsent(
            log.getType(), 
            i -> new ArrayList<>()
        );
        
        // Add to history
        list.add(log);
        
        // Limit history
        boolean resize = false;
        
        while (list.size() > 50) {
            list.remove(0);
            resize = true;
        }
        
        // Update to database
        if (resize) {
            // Replace history logs
            Nebula.getGameDatabase().update(
                this,
                this.getPlayerUid(),
                "histories." + log.getType(),
                list
            );
        } else {
            // Add to history list
            Nebula.getGameDatabase().addToSet(
                this,
                this.getPlayerUid(),
                "histories." + log.getType(),
                log
            );
        }
    }
}
