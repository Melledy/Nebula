package emu.nebula.data.resources;

import emu.nebula.data.BaseDef;
import emu.nebula.data.ResourceType;
import lombok.Getter;

@Getter
@ResourceType(name = "Honor.json")
public class HonorDef extends BaseDef {
    private int Id;
    private int Type;
    
    @Override
    public int getId() {
        return Id;
    }
}
