package emu.nebula.data.resources;

import emu.nebula.data.BaseDef;
import emu.nebula.data.ResourceType;
import emu.nebula.data.custom.StarTowerQuestParamDef;
import emu.nebula.game.inventory.ItemParamMap;
import emu.nebula.game.quest.QuestData;
import emu.nebula.game.quest.QuestType;
import lombok.Getter;

@Getter
@ResourceType(name = "StarTowerQuest.json")
public class StarTowerQuestDef extends BaseDef implements QuestData {
    private int Id;
    private String Title;
    private int TowerQuestType; // 1 = Core, 2 = Normal
    private int[] PreTowerQuestIds;
    
    // Rewards
    private int Reward1;
    private int RewardQty1;
    private int Reward2;
    private int RewardQty2;
    
    // Cached rewards
    private transient ItemParamMap rewards;
    
    // Custom params (loaded from defs/StarTowerQuestParam.json)
    private transient int completeCond;
    private transient int aim;
    private transient int param1;
    private transient int paramCond1;
    private transient int param2;
    private transient int paramCond2;
    private transient int[] condParams;
    
    @Override
    public int getId() {
        return Id;
    }
    
    @Override
    public int getQuestType() {
        return QuestType.Tower;
    }
    
    @Override
    public int getCompleteCond() {
        return this.completeCond;
    }
    
    @Override
    public int[] getCompleteCondParams() {
        if (this.condParams == null) {
            this.condParams = new int[] { 1, 0, 0 };
        }
        
        return this.condParams;
    }
    
    /**
     * Applies the generated condition params from the internal defs file.
     */
    public void setParams(StarTowerQuestParamDef params) {
        this.completeCond = params.getCompleteCond();
        this.aim = params.getAim();
        this.param1 = params.getParam1();
        this.paramCond1 = params.getParamCond1();
        this.param2 = params.getParam2();
        this.paramCond2 = params.getParamCond2();
        
        this.condParams = new int[] { this.aim, this.param1, this.param2 };
    }
    
    @Override
    public void onLoad() {
        // Quests without prerequisites have no array in the data file
        if (this.PreTowerQuestIds == null) {
            this.PreTowerQuestIds = new int[0];
        }
        
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
