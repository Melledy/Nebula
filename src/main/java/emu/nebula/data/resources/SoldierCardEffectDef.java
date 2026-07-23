package emu.nebula.data.resources;

import java.util.List;

import emu.nebula.data.BaseDef;
import emu.nebula.data.ResourceType;
import lombok.Getter;

@Getter
@ResourceType(name = "SoldierCardEffect.json")
public class SoldierCardEffectDef extends BaseDef {
    private int Id;
    private List<Integer> BuffIds;

    @Override
    public int getId() {
        return Id;
    }
}
