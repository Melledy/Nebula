package emu.nebula.data.resources;

import emu.nebula.data.BaseDef;
import emu.nebula.data.ResourceType;
import lombok.Getter;

@Getter
@ResourceType(name = "NPCAffinityPlot.json")
public class NpcAffinityPlotDef extends BaseDef {
    private int Id;
    private int NPCId;
    private int AffinityLevel;
    private int ItemId;
    private int ItemQty;
    
    @Override
    public int getId() {
        return Id;
    }
}
