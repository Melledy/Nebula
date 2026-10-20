package emu.nebula.data.resources;

import emu.nebula.data.BaseDef;
import emu.nebula.data.ResourceType;
import lombok.Getter;

@Getter
@ResourceType(name = "DormQuestGroup.json")
public class DormQuestGroupDef extends BaseDef {
    private int Id;
    private int PreGroup;
    private String Title;
    private int Item1;
    private int Qty1;
    private int Item2;
    private int Qty2;
    private int Item3;
    private int Qty3;

    @Override
    public int getId() {
        return Id;
    }
}
