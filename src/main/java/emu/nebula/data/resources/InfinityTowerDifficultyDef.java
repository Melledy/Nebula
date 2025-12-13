package emu.nebula.data.resources;

import emu.nebula.data.BaseDef;
import emu.nebula.data.ResourceType;

import lombok.Getter;

@Getter
@ResourceType(name = "InfinityTowerDifficulty.json")
public class InfinityTowerDifficultyDef extends BaseDef {
    private int Id;
    private int TowerId;
    
    @Override
    public int getId() {
        return Id;
    }
    
}
