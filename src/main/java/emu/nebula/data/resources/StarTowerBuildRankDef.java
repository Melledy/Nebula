package emu.nebula.data.resources;

import emu.nebula.data.BaseDef;
import emu.nebula.data.ResourceType;
import lombok.Getter;

@Getter
@ResourceType(name = "StarTowerBuildRank.json")
public class StarTowerBuildRankDef extends BaseDef {
    private int Id;
    private int MinGrade;
    private int Level;
    private int Rarity;
    
    @Override
    public int getId() {
        return Id;
    }
}
