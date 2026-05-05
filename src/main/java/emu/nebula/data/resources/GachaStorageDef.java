package emu.nebula.data.resources;

import emu.nebula.data.BaseDef;
import emu.nebula.data.ResourceType;
import emu.nebula.data.ResourceType.LoadPriority;
import emu.nebula.game.inventory.ItemParamMap;
import lombok.Getter;

@Getter
@ResourceType(name = "GachaStorage.json", loadPriority = LoadPriority.HIGH)
public class GachaStorageDef extends BaseDef {
    private int Id;
    
    private int DefaultId;
    private int DefaultQty;
    private int CostId;
    private int CostQty;
    
    private int ATypeGroup;
    private int AUpGuaranteeTimes;
    private int ATypeUpProb;
    private int ATypeUpShowProb;
    
    private int BTypeProb;
    private int BGuaranteeTimes;
    private int BTypeUpProb;
    private int BTypeUpShowProb;
    private int BTypeGuaranteeProb;
    
    private String GiveItems;
    private transient ItemParamMap giveItemsMap = new ItemParamMap();
    
    @Override
    public int getId() {
        return Id;
    }
    
    @Override
    public void onLoad() {
        if (this.GiveItems != null && !this.GiveItems.isEmpty()) {
            this.giveItemsMap = ItemParamMap.fromJsonString(this.GiveItems);
        }
    }
}
