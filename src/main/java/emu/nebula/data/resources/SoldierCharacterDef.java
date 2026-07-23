package emu.nebula.data.resources;

import emu.nebula.data.BaseDef;
import emu.nebula.data.ResourceType;
import lombok.Getter;

@Getter
@ResourceType(name = "SoldierCharacter.json")
public class SoldierCharacterDef extends BaseDef {
    private int Id;
    private int GroupId;
    private int Rarity;
    private int Cost;
    private int MaxStar;
    private int CharacterType;
    private boolean BoardChess;

    @Override
    public int getId() {
        return Id;
    }
}
