package emu.nebula.data.resources;

import emu.nebula.data.BaseDef;
import emu.nebula.data.ResourceType;
import lombok.Getter;

@Getter
@ResourceType(name = "DormQuest.json")
public class DormQuestDef extends BaseDef {
    private int Id;
    private int DormQuestGroup;
    private String Title;
    private int Reward1;
    private int RewardQty1;
    private int Reward2;
    private int RewardQty2;

    @Override
    public int getId() {
        return Id;
    }
}
