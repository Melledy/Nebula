package emu.nebula.data.resources;

import emu.nebula.data.BaseDef;
import emu.nebula.data.ResourceType;
import lombok.Getter;

@Getter
@ResourceType(name = "GachaNewbie.json")
public class GachaNewbieDef extends BaseDef {
    private int Id;
    private int SpinCount;
    private int SaveCount;

    @Override
    public int getId() {
        return this.Id;
    }

}
