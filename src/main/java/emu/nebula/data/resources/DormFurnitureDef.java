package emu.nebula.data.resources;

import emu.nebula.data.BaseDef;
import emu.nebula.data.ResourceType;
import lombok.Getter;

@Getter
@ResourceType(name = "DormFurniture.json")
public class DormFurnitureDef extends BaseDef {
    private int Id;
    private int CollectionValue;

    @Override
    public int getId() {
        return Id;
    }
}
