package emu.nebula.data.resources;

import emu.nebula.data.BaseDef;
import emu.nebula.data.ResourceType;

import lombok.Getter;

@Getter
@ResourceType(name = "ScoreBossReward.json")
public class ScoreBossRewardDef extends BaseDef {
    private int StarNeed;
    private int RewardItemId1;
    private int RewardNum1;
    
    @Override
    public int getId() {
        return StarNeed;
    }
}
