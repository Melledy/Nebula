package emu.nebula.data.resources;

import emu.nebula.data.BaseDef;
import emu.nebula.data.ResourceType;
import lombok.Getter;

@Getter
@ResourceType(name = "TraceHuntSelfHuntExtraCost.json")
public class TraceHuntSelfHuntExtraCostDef extends BaseDef {
    private int Times;
    private int ExtraCost1Tid;
    private int ExtraCost1Qty;
    
    @Override
    public int getId() {
        return Times;
    }
}
