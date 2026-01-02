package emu.nebula.game.inventory;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.Getter;

public enum ItemType {
    Res           (1),
    Item          (2),
    Char          (3),
    Energy        (4),
    WorldRankExp  (5),
    RogueItem     (6),
    Disc          (7),
    Equipment     (9),
    CharacterSkin (10),
    MonthlyCard   (11),
    Title         (12),
    Honor         (13),
    HeadItem      (14);

    @Getter
    private final int value;
    private final static Int2ObjectMap<ItemType> map = new Int2ObjectOpenHashMap<>();

    static {
        for (ItemType type : ItemType.values()) {
            map.put(type.getValue(), type);
        }
    }

    private ItemType(int value) {
        this.value = value;
    }

    public static ItemType getByValue(int value) {
        return map.get(value);
    }
}
