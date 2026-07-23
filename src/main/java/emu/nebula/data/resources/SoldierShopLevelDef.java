package emu.nebula.data.resources;

import emu.nebula.data.BaseDef;
import emu.nebula.data.ResourceType;
import lombok.Getter;

@Getter
@ResourceType(name = "SoldierShopLevel.json")
public class SoldierShopLevelDef extends BaseDef {
    private int Level;
    private int Exp;
    private int Count;
    private int Rarity1;
    private int Rarity2;
    private int Rarity3;
    private int Rarity4;
    private int Rarity5;

    @Override
    public int getId() {
        return Level;
    }

    public int rarityWeight(int rarity) {
        return switch (rarity) {
            case 1 -> Rarity1;
            case 2 -> Rarity2;
            case 3 -> Rarity3;
            case 4 -> Rarity4;
            case 5 -> Rarity5;
            default -> 0;
        };
    }
}
