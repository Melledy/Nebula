package emu.nebula.data.resources;

import emu.nebula.data.BaseDef;
import emu.nebula.data.ResourceType;
import emu.nebula.game.inventory.ItemParamMap;
import lombok.Getter;

@Getter
@ResourceType(name = "TowerDefenseLevel.json")
public class TowerDefenseLevelDef extends BaseDef {
    private int Id;
    private int Condition2;
    private int Condition3;
    private int Item1;
    private int Qty1;
    private int Item2;
    private int Qty2;

    private transient ItemParamMap rewards;
    
    @Override
    public int getId() {
        return Id;
    }

    @Override
    public void onLoad() {
        // Parse rewards
        this.rewards = new ItemParamMap();
        this.rewards.add(this.Item1, this.Qty1);
        this.rewards.add(this.Item2, this.Qty2);
    }
}
