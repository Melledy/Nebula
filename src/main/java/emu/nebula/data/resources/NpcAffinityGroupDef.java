package emu.nebula.data.resources;

import emu.nebula.data.BaseDef;
import emu.nebula.data.ResourceType;
import lombok.Getter;

@Getter
@ResourceType(name = "NPCAffinityGroup.json")
public class NpcAffinityGroupDef extends BaseDef {
    private int Id;
    private int Level;
    private int AffinityValue;
    private int AffinityGroupId;
    
    @Override
    public int getId() {
        return Id;
    }
}
