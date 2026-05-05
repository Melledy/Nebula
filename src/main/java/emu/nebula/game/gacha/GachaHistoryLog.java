package emu.nebula.game.gacha;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Indexed;
import emu.nebula.Nebula;
import emu.nebula.proto.GachaHistoriesOuterClass.GachaHistory;
import it.unimi.dsi.fastutil.ints.IntList;
import lombok.Getter;
import org.bson.types.ObjectId;

@Getter
@Entity(value = "gacha_history", useDiscriminator = false)
public class GachaHistoryLog {

    @Id
    private ObjectId id;

    @Indexed
    private int playerUid;

    @Indexed
    private int type;

    private int gid;

    @Indexed
    private long time;

    private IntList ids;
    
    @Deprecated // Morphia only
    public GachaHistoryLog() {
        
    }
    
    public GachaHistoryLog(int playerUid, int type, int gachaId, IntList results) {
        this.playerUid = playerUid;
        this.type = type;
        this.gid = gachaId;
        this.time = Nebula.getCurrentServerTime();
        this.ids = results;
    }

    // Proto
    public GachaHistory toProto() {
        var proto = GachaHistory.newInstance()
                .setGid(this.getGid())
                .setTime(this.getTime());
        
        for (int id : this.getIds()) {
            proto.addIds(id);
        }
        
        return proto;
    }
}
