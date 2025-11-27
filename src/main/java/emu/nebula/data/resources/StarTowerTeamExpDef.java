package emu.nebula.data.resources;

import emu.nebula.data.BaseDef;
import emu.nebula.data.ResourceType;
import lombok.Getter;

@Getter
@ResourceType(name = "StarTowerTeamExp.json")
public class StarTowerTeamExpDef extends BaseDef {
    private int Id;
    private int GroupId;
    private int Level;
    private int NeedExp;
    
    @Override
    public int getId() {
        return Id;
    }
}
