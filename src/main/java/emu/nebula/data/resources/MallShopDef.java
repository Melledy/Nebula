package emu.nebula.data.resources;

import com.google.gson.annotations.SerializedName;

import emu.nebula.data.BaseDef;
import emu.nebula.data.ResourceType;
import emu.nebula.game.inventory.ItemParamMap;
import emu.nebula.game.player.Player;
import lombok.Getter;

@Getter
@ResourceType(name = "MallShop.json")
public class MallShopDef extends BaseDef {
    @SerializedName("Id")
    private String IdString;
    private int Stock;
    
    private int ExchangeItemId;
    private int ExchangeItemQty;
    
    private int ItemId;
    private int ItemQty;
    
    private transient ItemParamMap products;
    
    @Override
    public int getId() {
        return IdString.hashCode();
    }
    
    public int getStock(Player player) {
        int limit = this.getStock();
        if (limit <= 0) return 0;
        int bought = player.getInventory().getMallBuyCount().getOrDefault(this.getIdString(), 0);
        return limit - (bought % limit);
    }
    
    @Override
    public void onLoad() {
        this.products = new ItemParamMap();
        
        if (this.ItemId > 0) {
            this.products.add(this.ItemId, this.ItemQty);
        }
    }
}
