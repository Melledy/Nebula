package emu.nebula.data.resources;

import emu.nebula.data.BaseDef;
import emu.nebula.data.ResourceType;
import lombok.Getter;

@Getter
@ResourceType(name = "TraceHuntControl.json")
public class TraceHuntControlDef extends BaseDef {
    private int Id;
    private int[] BossList;
    
    @Override
    public int getId() {
        return Id;
    }
}
