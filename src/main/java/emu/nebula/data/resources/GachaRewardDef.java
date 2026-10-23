package emu.nebula.data.resources;

import emu.nebula.data.BaseDef;
import emu.nebula.data.ResourceType;
import lombok.Getter;

@Getter
@ResourceType(name = "GachaReward.json")
public class GachaRewardDef extends BaseDef {
    private int Id;
    private int GachaId;
    private int Index;
    private int Num;
    private int Tid;
    private int Qty;
    
    @Override
    public int getId() {
        return this.Id;
    }

}
