package emu.nebula.data.resources;

import emu.nebula.data.BaseDef;
import emu.nebula.data.GameData;
import emu.nebula.data.ResourceType;
import lombok.Getter;

@Getter
@ResourceType(name = "Honor.json")
public class HonorDef extends BaseDef {
    private int Id;
    private int Type;
    private int[] Params;
    
    @Override
    public int getId() {
        return Id;
    }
    
    public boolean isValid() {
        if (this.Type == 2) {
            if (this.Params.length < 1) {
                return false;
            }
            
            int charId = this.Params[0];
            var charData = GameData.getCharacterDataTable().get(charId);
            
            if (charData == null || !charData.isAvailable()) {
                return false;
            }
        }
        
        return true;
    }
}
