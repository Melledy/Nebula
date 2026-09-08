package emu.nebula.data.resources;

import emu.nebula.data.BaseDef;
import emu.nebula.data.ResourceType;
import emu.nebula.game.inventory.ItemParamMap;
import lombok.Getter;

@Getter
@ResourceType(name = "ActivityStory.json")
public class ActivityStoryDef extends BaseDef {
    private int Id;
    private int ChapterId;
    
    private String FirstCompleteReward;
    
    private transient ItemParamMap rewards;
    
    @Override
    public int getId() {
        return Id;
    }
    
    @Override
    public void onLoad() {
        this.rewards = ItemParamMap.fromJsonString(this.FirstCompleteReward);
    }
}
