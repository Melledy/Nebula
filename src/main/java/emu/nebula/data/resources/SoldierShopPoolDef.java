package emu.nebula.data.resources;

import emu.nebula.data.BaseDef;
import emu.nebula.data.ResourceType;
import lombok.Getter;

@Getter
@ResourceType(name = "SoldierShopPool.json")
public class SoldierShopPoolDef extends BaseDef {
    private int Id;
    private int GroupId;
    private int ChessCharacterId;
    private int Rarity;
    private int Weight;
    private int Cost;

    @Override
    public int getId() {
        return Id;
    }
}
