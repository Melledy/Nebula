package emu.nebula.data.resources;

import emu.nebula.data.BaseDef;
import emu.nebula.data.ResourceType;
import lombok.Getter;

@Getter
@ResourceType(name = "DormRoleSkin.json")
public class DormRoleSkinDef extends BaseDef {
    private int Id;
    private int TrId;
    private int TrQty;
    private int UnlockChar;

    @Override
    public int getId() {
        return Id;
    }
}
