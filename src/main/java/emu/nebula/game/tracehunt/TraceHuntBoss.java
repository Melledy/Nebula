package emu.nebula.game.tracehunt;

import dev.morphia.annotations.Entity;
import emu.nebula.proto.Public.TraceHuntBossCollection;
import lombok.Getter;

@Getter
@Entity(useDiscriminator = false)
public class TraceHuntBoss {
    private int id;
    private int hunts;
    private int assists;
    
    @Deprecated // Morphia only
    public TraceHuntBoss() {
        
    }
    
    public TraceHuntBoss(int bossId) {
        this.id = bossId;
    }

    public void incrementHuntCount() {
        this.hunts++;
    }

    public void incrementAssistCount() {
        this.assists++;
    }
    
    // Proto

    public TraceHuntBossCollection toProto() {
        var proto = TraceHuntBossCollection.newInstance()
                .setId(this.getId())
                .setHuntCount(this.hunts)
                .setAssistHuntCount(this.assists);
        
        return proto;
    }
}
