package emu.nebula.data.resources;

import emu.nebula.data.BaseDef;
import emu.nebula.data.ResourceType;
import emu.nebula.game.inventory.ItemParamMap;
import emu.nebula.game.player.Player;
import lombok.Getter;

@Getter
@ResourceType(name = "ResidentGoods.json")
public class ResidentGoodsDef extends BaseDef {
    private int Id;
    private int ShopId;
    private int MaximumLimit;
    
    private int ItemId;
    private int ItemQuantity;

    private int CurrencyItemId;
    private int Price;
    
    private transient ItemParamMap products;
    
    @Override
    public int getId() {
        return Id;
    }

    public int getStock(Player player) {
        int limit = this.getMaximumLimit();
        if (limit <= 0) return 0;
        int bought = player.getInventory().getShopBuyCount().getOrDefault(this.getId(), 0);
        return limit - (bought % limit);
    }
    
    @Override
    public void onLoad() {
        this.products = new ItemParamMap();
        
        if (this.ItemId > 0) {
            this.products.add(this.ItemId, this.ItemQuantity);
        }
    }
}
