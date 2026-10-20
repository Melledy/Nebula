package emu.nebula.data.resources;

import emu.nebula.data.BaseDef;
import emu.nebula.data.ResourceType;
import lombok.Getter;

@Getter
@ResourceType(name = "DormRole.json")
public class DormRoleDef extends BaseDef {
    private int Id;
    private int RoleType;
    private int[] Exclusions;

    @Override
    public int getId() {
        return Id;
    }
}
