package emu.nebula.data.resources;

import emu.nebula.data.BaseDef;
import emu.nebula.data.ResourceType;

import lombok.Getter;

@Getter
@ResourceType(name = "Drop.json")
public class DropDef extends BaseDef {
    private int DropId;
    private int PkgId;
    
    @Override
    public int getId() {
        return DropId;
    }
    
    @Override
    public void onLoad() {
        
    }
}
