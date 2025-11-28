package emu.nebula.data.resources;

import emu.nebula.data.BaseDef;
import emu.nebula.data.ResourceType;
import lombok.Getter;

@Getter
@ResourceType(name = "StarTowerFloorExp.json")
public class StarTowerFloorExpDef extends BaseDef {
    private int Id;
    private int StarTowerId;
    private int Stage;
    private int NormalExp;
    private int EliteExp;
    private int BossExp;
    private int FinalBossExp;
    
    @Override
    public int getId() {
        return Id;
    }
}
