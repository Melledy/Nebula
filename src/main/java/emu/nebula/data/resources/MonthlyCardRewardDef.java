package emu.nebula.data.resources;

import emu.nebula.data.BaseDef;
import emu.nebula.data.ResourceType;
import emu.nebula.game.inventory.ItemParamMap;
import lombok.Getter;

@Getter
@ResourceType(name = "MonthlyCard.json")
public class MonthlyCardRewardDef extends BaseDef {
    private int Id;
    private int CardId;
    private int RewardId1;
    private int RewardNum1;
    private int RewardId2;
    private int RewardNum2;

    private transient ItemParamMap rewards;

    @Override
    public int getId() {
        return this.Id;
    }

    @Override
    public void onLoad() {
        this.rewards = new ItemParamMap();

        if (this.RewardId1 > 0 && this.RewardNum1 > 0) {
            this.rewards.add(this.RewardId1, this.RewardNum1);
        }
        if (this.RewardId2 > 0 && this.RewardNum2 > 0) {
            this.rewards.add(this.RewardId2, this.RewardNum2);
        }
    }

}
