package emu.nebula.data.resources;

import emu.nebula.data.BaseDef;
import emu.nebula.data.ResourceType;
import emu.nebula.game.inventory.ItemParamMap;
import lombok.Getter;

@Getter
@ResourceType(name = "DormQuestGroup.json")
public class DormQuestGroupDef extends BaseDef {
    private int Id;
    private int PreGroup;
    private String Title;
    private int Item1;
    private int Qty1;
    private int Item2;
    private int Qty2;
    private int Item3;
    private int Qty3;

    // Cached rewards
    private transient ItemParamMap rewards;

    @Override
    public int getId() {
        return Id;
    }

    @Override
    public void onLoad() {
        // Cache rewards into an item param map
        this.rewards = new ItemParamMap();

        if (this.Item1 > 0) {
            this.rewards.add(this.Item1, this.Qty1);
        }

        if (this.Item2 > 0) {
            this.rewards.add(this.Item2, this.Qty2);
        }

        if (this.Item3 > 0) {
            this.rewards.add(this.Item3, this.Qty3);
        }
    }
}
