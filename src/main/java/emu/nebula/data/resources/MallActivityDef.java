package emu.nebula.data.resources;

import com.google.gson.annotations.SerializedName;

import emu.nebula.data.BaseDef;
import emu.nebula.data.ResourceType;

import lombok.Getter;

@Getter
@ResourceType(name = "MallActivity.json")
public class MallActivityDef extends BaseDef {
    @SerializedName("Id")
    private String IdString;
    private String Name;
    private int Price;
    private String ShowPrice;
    private int ActivityId;
    
    @Override
    public int getId() {
        return IdString.hashCode();
    }
    
}
