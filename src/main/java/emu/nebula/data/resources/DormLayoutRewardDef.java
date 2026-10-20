package emu.nebula.data.resources;

import emu.nebula.data.BaseDef;
import emu.nebula.data.ResourceType;
import lombok.Getter;

@Getter
@ResourceType(name = "DormLayoutReward.json")
public class DormLayoutRewardDef extends BaseDef {
    private int Id;
    private int RoomType;
    private int FurnitureCount;
    private int RewardId1;
    private int Qty1;

    @Override
    public int getId() {
        return Id;
    }
}
