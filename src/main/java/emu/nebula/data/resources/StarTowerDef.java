package emu.nebula.data.resources;

import java.util.Arrays;

import emu.nebula.data.BaseDef;
import emu.nebula.data.ResourceType;
import lombok.Getter;

@Getter
@ResourceType(name = "StarTower.json")
public class StarTowerDef extends BaseDef {
    private int Id;
    private int[] FloorNum;
    
    private transient int maxFloors;
    
    @Override
    public int getId() {
        return Id;
    }

    public int getMaxFloor(int stage) {
        int index = stage - 1;
        
        if (index < 0 || index >= this.FloorNum.length) {
            return 0;
        }
        
        return this.FloorNum[index];
    }
    
    @Override
    public void onLoad() {
        this.maxFloors = Arrays.stream(this.FloorNum).reduce(0, Integer::sum);
    }
}
