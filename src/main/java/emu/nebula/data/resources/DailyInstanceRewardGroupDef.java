package emu.nebula.data.resources;

import emu.nebula.data.BaseDef;
import emu.nebula.data.ResourceType;
import emu.nebula.game.inventory.ItemRewardList;
import emu.nebula.game.inventory.ItemRewardParam;
import emu.nebula.util.JsonUtils;

import lombok.Getter;

@Getter
@ResourceType(name = "DailyInstanceRewardGroup.json")
public class DailyInstanceRewardGroupDef extends BaseDef {
    private int GroupId;
    private int DailyRewardType;
    private String BaseAwardPreview;
    
    // Parsed rewards
    private transient ItemRewardList firstRewards;
    private transient ItemRewardList rewards;
    
    @Override
    public int getId() {
        return GroupId + DailyRewardType;
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
}
