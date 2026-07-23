package emu.nebula.data.resources;

import emu.nebula.data.BaseDef;
import emu.nebula.data.ResourceType;
import lombok.Getter;

@Getter
@ResourceType(name = "SoldierEventPlan.json")
public class SoldierEventPlanDef extends BaseDef {
    private int Id;
    private int EventGroupId;
    private int EventType;

    @Override
    public int getId() {
        return Id;
    }
}
