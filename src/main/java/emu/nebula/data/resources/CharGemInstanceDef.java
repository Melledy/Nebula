package emu.nebula.data.resources;

import emu.nebula.data.BaseDef;
import emu.nebula.data.GameData;
import emu.nebula.data.ResourceType;
import emu.nebula.game.instance.InstanceData;
import emu.nebula.game.inventory.ItemRewardList;
import emu.nebula.game.inventory.ItemRewardParam;
import emu.nebula.util.JsonUtils;

import lombok.Getter;

@Getter
@ResourceType(name = "CharGemInstance.json")
public class CharGemInstanceDef extends BaseDef implements InstanceData {
    private int Id;
    private int PreLevelId;
    private int PreLevelStar;
    private int NeedWorldClass;
    private int EnergyConsume;
    private int DropId;
    private String BaseAwardPreview;
    
    private transient ItemRewardList firstRewards;
    private transient ItemRewardList rewards;
    
    @Override
    public int getId() {
        return Id;
    }
    
    @Override
    public void onLoad() {
        // Init reward lists
        this.firstRewards = new ItemRewardList();
        this.rewards = new ItemRewardList();
        
        // Parse rewards
        var awards = JsonUtils.decodeList(this.BaseAwardPreview, int[].class);
        if (awards == null) {
            return;
        }
        
        for (int[] award : awards) {
            int itemId = award[0];
            int min = award[1];
            int max = award.length >= 4 ? award[2] : min;
            boolean isFirst = award[award.length - 1] == 1;
            
            if (min == -1) {
                min = 0;
                max = 1;
            }
            
            var reward = new ItemRewardParam(itemId, min, max);
            
            if (isFirst) {
                this.firstRewards.add(reward);
            } else {
                this.rewards.add(reward);
            }
        }
    }
    
    public CharGemInstanceRewardGroupDef getRewardGroup(int rewardType) {
        // Instances without a drop id don't have selectable rewards
        if (this.DropId <= 0) {
            return null;
        }
        
        int groupId = this.getDropId() + rewardType;
        return GameData.getCharGemInstanceRewardGroupDataTable().get(groupId);
    }
    
    @Override
    public ItemRewardList getFirstRewards(int rewardType) {
        var data = this.getRewardGroup(rewardType);
        
        if (data != null) {
            return data.getFirstRewards();
        } else {
            return this.firstRewards;
        }
    }
    
    @Override
    public ItemRewardList getRewards(int rewardType) {
        var data = this.getRewardGroup(rewardType);
        
        if (data != null) {
            return data.getRewards();
        } else {
            return this.rewards;
        }
    }
}
