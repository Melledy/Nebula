package emu.nebula.data.resources;

import java.util.Arrays;

import emu.nebula.data.BaseDef;
import emu.nebula.data.ResourceType;
import emu.nebula.data.ResourceType.LoadPriority;
import emu.nebula.game.quest.QuestData;
import emu.nebula.game.quest.QuestType;

import lombok.Getter;

@Getter
@ResourceType(name = "StarTowerBookEventReward.json", loadPriority = LoadPriority.LOW)
public class StarTowerBookEventRewardDef extends BaseDef implements QuestData {
    private int Id;
    private int Type; // 1 = Character clear count, 2 = NPC affinity level
    private int NPCId;
    private int Sort;
    private int CompleteCond;
    private String CompleteCondParams;
    private int ItemId;
    private int ItemQty;
    
    // Cached data
    private transient int[] condParams;
    
    @Override
    public int getId() {
        return this.Id;
    }
    
    @Override
    public int getQuestType() {
        return QuestType.TowerEvent;
    }
    
    @Override
    public int getCompleteCond() {
        return this.CompleteCond;
    }
    
    @Override
    public int[] getCompleteCondParams() {
        if (this.condParams == null) {
            this.condParams = new int[] { 1, 0 };
        }
        
        return this.condParams;
    }
    
    @Override
    public void onLoad() {
        // Flip params
        // The data stores params as [target id, required amount]
        // while GameQuest expects [required amount, target id]
        if (this.CompleteCondParams != null && !this.CompleteCondParams.isBlank()) {
            var params = Arrays.stream(this.CompleteCondParams.replaceAll("[\\[\\] ]", "")
                    .split(","))
                    .filter(value -> !value.isEmpty())
                    .mapToInt(Integer::parseInt)
                    .toArray();
            
            if (params.length >= 2) {
                this.condParams = new int[] { params[1], params[0] };
            }
        }
        
        if (this.condParams == null) {
            this.condParams = new int[] { 1, 0 };
        }
    }
}
