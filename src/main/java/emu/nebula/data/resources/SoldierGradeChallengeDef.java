package emu.nebula.data.resources;

import emu.nebula.data.BaseDef;
import emu.nebula.data.ResourceType;
import lombok.Getter;

@Getter
@ResourceType(name = "SoldierGradeChallenge.json")
public class SoldierGradeChallengeDef extends BaseDef {
    private int Id;
    private int KeyGradeId;
    private int GradeLevel;
    private int NodeGroupId;
    private int Score;
    private int UnlockGradeLevel;

    @Override
    public int getId() {
        return Id;
    }
}
