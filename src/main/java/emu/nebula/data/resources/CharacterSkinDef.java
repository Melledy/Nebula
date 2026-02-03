package emu.nebula.data.resources;

import emu.nebula.data.BaseDef;
import emu.nebula.data.ResourceType;
import lombok.Getter;
import lombok.Setter;

@Getter
@ResourceType(name = "CharacterSkin.json")
public class CharacterSkinDef extends BaseDef {
    private int Id;
    private int CharId;
    private int Type;
    
    @Setter
    private transient boolean released;
    
    @Override
    public int getId() {
        return Id;
    }
}
