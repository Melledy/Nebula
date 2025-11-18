package emu.nebula.data.resources;

import emu.nebula.data.BaseDef;
import emu.nebula.data.ResourceType;
import lombok.Getter;

@Getter
@ResourceType(name = "StarTowerGrowthNode.json")
public class StarTowerGrowthNodeDef extends BaseDef {
    private int Id;
    private int NodeId;
    private int Group;
    
    private int ItemId1;
    private int ItemQty1;
    
    @Override
    public int getId() {
        return Id;
    }
}
