package emu.nebula.data.resources;

import emu.nebula.data.BaseDef;
import emu.nebula.data.ResourceType;
import lombok.Getter;

@Getter
@ResourceType(name = "SoldierNodePlan.json")
public class SoldierNodePlanDef extends BaseDef {
    private int Id;
    private int NodeGroupId;
    private int Stage;
    private int Index;
    private int NodeType;
    private int EventGroupId;
    private int DifficultyLevelAdd;
    private int Coin;
    private int Experience;
    private int AddHp;
    private int LoseHp;

    @Override
    public int getId() {
        return Id;
    }
}
