package emu.nebula.data.resources;

import emu.nebula.data.BaseDef;
import emu.nebula.data.ResourceType;
import emu.nebula.data.custom.DormQuestParamDef;
import emu.nebula.game.inventory.ItemParamMap;
import emu.nebula.game.quest.QuestData;
import emu.nebula.game.quest.QuestType;
import lombok.Getter;

@Getter
@ResourceType(name = "DormQuest.json")
public class DormQuestDef extends BaseDef implements QuestData {
    private int Id;
    private int DormQuestGroup;
    private String Title;
    private int Reward1;
    private int RewardQty1;
    private int Reward2;
    private int RewardQty2;
    
    // Cached rewards
    private transient ItemParamMap rewards;
    
    // Custom params (loaded from defs/DormQuestParam.json)
    private transient int completeCond;
    private transient int[] condParams;

    @Override
    public int getId() {
        return Id;
    }
    
    @Override
    public int getQuestType() {
        return QuestType.Dorm;
    }
    
    @Override
    public int getCompleteCond() {
        return this.completeCond;
    }
    
    @Override
    public int[] getCompleteCondParams() {
        if (this.condParams == null) {
            this.condParams = new int[] { 1 };
        }
        
        return this.condParams;
    }
    
    /**
     * Applies the condition params from the internal defs file.
     */
    public void setParams(DormQuestParamDef params) {
        this.completeCond = params.getCompleteCond();
        this.condParams = params.getCondParams();
    }
    
    @Override
    public void onLoad() {
        // Cache rewards into an item param map
        this.rewards = new ItemParamMap();
        
        if (this.Reward1 > 0) {
            this.rewards.add(this.Reward1, this.RewardQty1);
        }
        
        if (this.Reward2 > 0) {
            this.rewards.add(this.Reward2, this.RewardQty2);
        }
    }
}
