package emu.nebula.game.tracehunt;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.Getter;

public enum TraceHuntLogType {
    TraceStart      (1),
    Tracing         (2),
    TraceBeforeEnd  (3),
    TraceEnd        (4),
    TraceInterrupt  (5),
    TraceRestart    (6),
    HuntBeforeStart (7),
    HuntStart       (8),
    HuntPlayer      (9),
    HuntNPC         (10),
    HuntPlayerFatal (11),
    HuntNPCFatal    (12),
    HuntEnd         (13),
    Settlement      (14),
    HuntAfterStart  (15),
    HuntInterrupt   (16);

    @Getter
    private final int value;
    private final static Int2ObjectMap<TraceHuntLogType> map = new Int2ObjectOpenHashMap<>();

    static {
        for (TraceHuntLogType type : TraceHuntLogType.values()) {
            map.put(type.getValue(), type);
        }
    }

    private TraceHuntLogType(int value) {
        this.value = value;
    }

    public static TraceHuntLogType getByValue(int value) {
        return map.get(value);
    }
}
