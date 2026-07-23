package emu.nebula.data.resources;

import emu.nebula.data.BaseDef;
import emu.nebula.data.ResourceType;
import lombok.Getter;

@Getter
@ResourceType(name = "SoldierFloor.json")
public class SoldierFloorDef extends BaseDef {
    private int Id;
    private int Default;
    private int BattleTime;

    @Override
    public int getId() {
        return Id;
    }
}
