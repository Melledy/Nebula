package emu.nebula.data.resources;

import com.google.gson.annotations.SerializedName;

import emu.nebula.data.BaseDef;
import emu.nebula.data.ResourceType;
import lombok.Getter;

@Getter
@ResourceType(name = "MallGem.json")
public class MallGemDef extends BaseDef {
    @SerializedName("Id")
    private String IdString;
    
    private String Name;
    private String Desc;
    
    @SerializedName("BaseItemId")
    private int baseItemId;
    
    @SerializedName("BaseItemQty")
    private int baseItemQty;
    
    @SerializedName("ExperiencedBonusItemId")
    private int experiencedBonusItemId;
    
    @SerializedName("ExperiencedBonusItemQty")
    private int experiencedBonusItemQty;
    
    @SerializedName("MaidenBonusItemID")
    private int maidenBonusItemId;
    
    @SerializedName("MaidenBonusItemQty")
    private int maidenBonusItemQty;
    
    private int Price;
    
    @Override
    public int getId() {
        return IdString.hashCode();
    }
}
