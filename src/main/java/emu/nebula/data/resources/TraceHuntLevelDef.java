package emu.nebula.data.resources;

import emu.nebula.data.BaseDef;
import emu.nebula.data.ResourceType;
import lombok.Getter;

@Getter
@ResourceType(name = "TraceHuntLevel.json")
public class TraceHuntLevelDef extends BaseDef {
    private int Level;
    private int Exp;
    private int WorldClass;
    private int MaxStar;
    private int TokenRate;
    
    @Override
    public int getId() {
        return Level;
    }
}
