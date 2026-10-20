package emu.nebula.data.resources;

import emu.nebula.data.BaseDef;
import emu.nebula.data.ResourceType;
import lombok.Getter;

@Getter
@ResourceType(name = "DormHonorTitle.json")
public class DormHonorTitleDef extends BaseDef {
    private int Id;
    private int NeedExp;

    @Override
    public int getId() {
        return Id;
    }
}
