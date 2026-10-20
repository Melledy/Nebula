package emu.nebula.data.custom;

import emu.nebula.data.BaseDef;
import emu.nebula.data.GameData;
import emu.nebula.data.ResourceType;
import emu.nebula.data.ResourceType.LoadPriority;
import lombok.Getter;

@Getter
@ResourceType(name = "StarTowerQuestParam.json", useInternal = true, loadPriority = LoadPriority.LOW)
public class StarTowerQuestParamDef extends BaseDef {
    private int Id;
    
    private int CompleteCond;
    private int Aim;
    
    private int Param1;
    private int ParamCond1;
    private int Param2;
    private int ParamCond2;
    
    @Override
    public int getId() {
        return Id;
    }
    
    @Override
    public void onLoad() {
        var quest = GameData.getStarTowerQuestDataTable().get(this.Id);
        if (quest == null) {
            return;
        }
        
        quest.setParams(this);
    }
}
