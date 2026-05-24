package emu.nebula.data.resources;

import emu.nebula.data.BaseDef;
import emu.nebula.data.ResourceType;
import emu.nebula.game.inventory.ItemParamMap;
import emu.nebula.game.player.Player;
import emu.nebula.util.JsonUtils;
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
    private int AppearCondType;
    private String AppearCondParams;

    private transient ItemParamMap products;
    private transient int[] appearCondParams;

    @Override
    public int getId() {
        return Id;
    }

    public int getStock(Player player) {
        return Math.max(this.getMaximumLimit() - player.getInventory().getShopBuyCount().get(this.getId()), 0);
    }

    /**
     * Returns whether this resident shop good should currently be visible.
     */
    public boolean isVisible(Player player) {
        return ShopCondition.matchesResidentGoodsAppear(player, this.AppearCondType, this.appearCondParams);
    }
    
    @Override
    public void onLoad() {
        this.products = new ItemParamMap();
        this.appearCondParams = JsonUtils.decode(this.AppearCondParams, int[].class);
        
        if (this.ItemId > 0) {
            this.products.add(this.ItemId, this.ItemQuantity);
        }
    }

}
