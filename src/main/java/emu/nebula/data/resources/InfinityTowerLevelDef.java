package emu.nebula.data.resources;

import java.util.ArrayList;
import java.util.List;

import emu.nebula.data.BaseDef;
import emu.nebula.data.GameData;
import emu.nebula.data.ResourceType;
import emu.nebula.game.inventory.ItemParamMap;
import emu.nebula.game.inventory.ItemRewardParam;
import emu.nebula.util.JsonUtils;

import lombok.Getter;

@Getter
@ResourceType(name = "InfinityTowerLevel.json")
public class InfinityTowerLevelDef extends BaseDef {
    private int Id;
    private int DifficultyId;
    private String BaseAwardPreview;
    
    private transient List<ItemRewardParam> rewards;
    
    @Override
    public int getId() {
        return Id;
    }
    
    public int getTowerId() {
        var diff = GameData.getInfinityTowerDifficultyDataTable().get(this.DifficultyId);
        if (diff == null) {
            return 0;
        }
        
        return diff.getTowerId();
    }
    
    public ItemParamMap generateRewards() {
        var map = new ItemParamMap();
        
        for (var param : this.getRewards()) {
            map.add(param.getId(), param.getRandomCount());
        }
        
        return map;
    }
    
    @Override
    public void onLoad() {
        // Init reward lists
        this.rewards = new ArrayList<>();
        
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
                
            } else {
                this.rewards.add(reward);
            }
        }
    }
}
