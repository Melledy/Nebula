package emu.nebula.game.gacha;

import dev.morphia.annotations.Entity;
import emu.nebula.Nebula;
import emu.nebula.proto.GachaHistoriesOuterClass.GachaHistory;
import it.unimi.dsi.fastutil.ints.IntList;
import lombok.Getter;

@Getter
@Entity(value = "banner_info", useDiscriminator = false)
public class GachaHistoryLog {
    private int type;
    private long time;
    private IntList ids;
    
    @Deprecated // Morphia only
    public GachaHistoryLog() {
        
    }
    
    public GachaHistoryLog(int type, IntList results) {
        this.type = type;
        this.time = Nebula.getCurrentTime();
        this.ids = results;
    }
    
    // Proto
    
    public GachaHistory toProto() {
        var proto = GachaHistory.newInstance()
                .setGid(this.getType())
                .setTime(this.getTime());
        
        for (int id : this.getIds()) {
            proto.addIds(id);
        }
        
        return proto;
    }
}
