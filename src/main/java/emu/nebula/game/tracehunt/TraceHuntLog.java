package emu.nebula.game.tracehunt;

import dev.morphia.annotations.Entity;
import emu.nebula.proto.Public.TraceHuntLogEntry;
import lombok.Getter;

@Getter
@Entity(useDiscriminator = false)
public class TraceHuntLog {
    private int id;
    private String[] args;
    
    @Deprecated // Morphia only
    public TraceHuntLog() {
        
    }
    
    public TraceHuntLog(int id) {
        this.id = id;
    }
    
    public TraceHuntLog(int id, String... args) {
        this.id = id;
        this.args = args;
    }

    // Proto
    
    public TraceHuntLogEntry toProto() {
        var proto = TraceHuntLogEntry.newInstance()
                .setTid(this.getId());
        
        if (this.getArgs() != null) {
            proto.addAllArgs(this.getArgs());
        }
        
        return proto;
    }

}
