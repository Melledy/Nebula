package emu.nebula.data.resources;

import emu.nebula.data.BaseDef;
import emu.nebula.data.ResourceType;
import lombok.Getter;

@Getter
@ResourceType(name = "DormAchievement.json")
public class DormAchievementDef extends BaseDef {
    private int Id;
    private int CompleteCond;
    private int[] CompleteParams;
    private int Tid1;
    private int Qty1;
    private int Tid2;
    private int Qty2;
    private int Tid3;
    private int Qty3;

    @Override
    public int getId() {
        return Id;
    }
}
