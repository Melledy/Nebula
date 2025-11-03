package emu.nebula.data.resources;

import emu.nebula.data.BaseDef;
import emu.nebula.data.ResourceType;
import emu.nebula.game.inventory.ItemParamMap;
import emu.nebula.game.inventory.ItemSubType;
import emu.nebula.game.inventory.ItemType;
import lombok.Getter;
import lombok.Setter;

@Getter
@ResourceType(name = "Item.json")
public class ItemDef extends BaseDef {
    private int Id;
    private String Title;
    private int Type;
    private int Stype;
    private int Rarity;
    private boolean Stack;
    
    private int UseMode;
    private int UseAction;
    private String UseArgs;
    
    private transient ItemParamMap useParams;
    private transient ItemType itemType;
    private transient ItemSubType itemSubType;
    
    @Setter
    private transient TitleDef titleData;
    
    @Override
    public int getId() {
        return Id;
    }

    @Override
    public void onLoad() {
        this.itemType = ItemType.getByValue(this.Type);
        this.itemSubType = ItemSubType.getByValue(this.Stype);
        
        if (this.UseArgs != null) {
            this.useParams = ItemParamMap.fromJsonString(this.UseArgs);
        }
    }
}
