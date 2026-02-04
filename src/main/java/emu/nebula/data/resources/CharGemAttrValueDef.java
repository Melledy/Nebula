package emu.nebula.data.resources;

import emu.nebula.data.BaseDef;
import emu.nebula.data.GameData;
import emu.nebula.data.ResourceType;
import emu.nebula.data.ResourceType.LoadPriority;
import lombok.Getter;

@Getter
@ResourceType(name = "CharGemAttrValue.json", loadPriority = LoadPriority.LOW)
public class CharGemAttrValueDef extends BaseDef {
    private int Id;
    private int TypeId;
    private int AttrType;
    private int AttrTypeFirstSubtype;
    private int Rarity;
    
    @Override
    public int getId() {
        return Id;
    }
    
    @Override
    public void onLoad() {
        // Cache attribute values into attribute group defs
        // Honestly a horrible/inefficient way of doing this
        for (var data : GameData.getCharGemAttrGroupDataTable()) {
            for (var type : data.getAttributeTypes()) {
                // Skip if type id doesnt match
                if (type.getId() != this.getTypeId()) {
                    continue;
                }
                
                // Add
                type.addValue(this);
            }
        }
    }
}
