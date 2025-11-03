package emu.nebula.data.resources;

import emu.nebula.data.BaseDef;
import emu.nebula.data.GameData;
import emu.nebula.data.ResourceType;
import emu.nebula.data.ResourceType.LoadPriority;
import lombok.Getter;

@Getter
@ResourceType(name = "Title.json", loadPriority = LoadPriority.LOW)
public class TitleDef extends BaseDef {
    private int Id;
    private int ItemId;
    private int TitleType;
    
    @Override
    public int getId() {
        return Id;
    }
    
    @Override
    public void onLoad() {
        var item = GameData.getItemDataTable().get(this.getItemId());
        if (item != null) {
            item.setTitleData(this);
        }
    }
}
