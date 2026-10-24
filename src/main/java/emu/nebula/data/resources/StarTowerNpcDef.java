package emu.nebula.data.resources;

import emu.nebula.data.BaseDef;
import emu.nebula.data.ResourceType;
import lombok.Getter;

@Getter
@ResourceType(name = "StarTowerNPC.json")
public class StarTowerNpcDef extends BaseDef {
    private int Id;
    private int AffinityGroupId;
    
    @Override
    public int getId() {
        return Id;
    }
}
