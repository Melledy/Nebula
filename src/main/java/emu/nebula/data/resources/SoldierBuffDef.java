package emu.nebula.data.resources;

import java.util.List;

import emu.nebula.data.BaseDef;
import emu.nebula.data.ResourceType;
import lombok.Getter;

@Getter
@ResourceType(name = "SoldierBuff.json")
public class SoldierBuffDef extends BaseDef {
    private int Id;
    private int Cond1;
    private int Cond2;
    private int Effect1;
    private int Effect2;
    private List<String> EffectParams1;
    private List<String> EffectParams2;

    @Override
    public int getId() {
        return Id;
    }
}
