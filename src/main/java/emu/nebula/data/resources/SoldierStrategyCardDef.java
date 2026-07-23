package emu.nebula.data.resources;

import emu.nebula.data.BaseDef;
import emu.nebula.data.ResourceType;
import lombok.Getter;

@Getter
@ResourceType(name = "SoldierStrategyCard.json")
public class SoldierStrategyCardDef extends BaseDef {
    private int Id;
    private int GroupId;
    private int CardEffectId;
    private int GradeLevelCond;

    @Override
    public int getId() {
        return Id;
    }
}
