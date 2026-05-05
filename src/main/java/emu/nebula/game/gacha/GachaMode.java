package emu.nebula.game.gacha;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.Getter;

@Getter
public enum GachaMode {

    NEWBIE(0, 10),
    SINGLE(1, 1),
    TEN(2, 10);

    private static final Int2ObjectMap<GachaMode> GACHA_MODES = new Int2ObjectOpenHashMap<>();

    static {
        for (var gachaMode : values()) {
            GACHA_MODES.put(gachaMode.mode, gachaMode);
        }
    }

    private final int mode;
    private final int amount;

    GachaMode(int mode, int amount) {
        this.mode = mode;
        this.amount = amount;
    }

    public static GachaMode getGachaMode(int mode) {
        return GACHA_MODES.get(mode);
    }

    public static Integer getAmountByMode(int mode) {
        var gachaMode = getGachaMode(mode);
        return gachaMode != null ? gachaMode.amount : null;
    }
}
