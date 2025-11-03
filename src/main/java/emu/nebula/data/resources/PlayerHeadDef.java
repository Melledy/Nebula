package emu.nebula.data.resources;

import emu.nebula.data.BaseDef;
import emu.nebula.data.ResourceType;
import lombok.Getter;

@Getter
@ResourceType(name = "PlayerHead.json")
public class PlayerHeadDef extends BaseDef {
    private int Id;
    private int HeadType;
    private int UnlockChar;
    private int UnlockSkin;
    
    @Override
    public int getId() {
        return Id;
    }
}
