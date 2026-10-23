package emu.nebula.data.custom;

import emu.nebula.data.BaseDef;
import emu.nebula.data.GameData;
import emu.nebula.data.ResourceType;
import emu.nebula.data.ResourceType.LoadPriority;
import lombok.Getter;

@Getter
@ResourceType(name = "DormQuestParam.json", useInternal = true, loadPriority = LoadPriority.LOW)
public class DormQuestParamDef extends BaseDef {
    private int Id;
    
    private int CompleteCond;
    private int[] CondParams;
    
    @Override
    public int getId() {
        return Id;
    }
    
    @Override
    public void onLoad() {
        var quest = GameData.getDormQuestDataTable().get(this.Id);
        if (quest == null) {
            return;
        }
        
        quest.setParams(this);
    }
}
