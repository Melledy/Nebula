package emu.nebula.data.resources;

import com.google.gson.annotations.SerializedName;
import emu.nebula.GameConstants;
import emu.nebula.Nebula;
import emu.nebula.data.BaseDef;
import emu.nebula.data.ResourceType;
import emu.nebula.game.inventory.ItemParamMap;
import emu.nebula.game.player.Player;
import emu.nebula.util.Utils;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ResourceType(name = "MallShop.json")
public class MallShopDef extends BaseDef {
    @SerializedName("Id")
    private String IdString;
    private int Stock;
    
    private int ExchangeItemId;
    private int ExchangeItemQty;
    
    private int ItemId;
    private int ItemQty;
    private String ListTime;
    private String DeListTime;
    private int RefreshType;
    
    private transient ItemParamMap products;
    private transient long listTimeSeconds;
    private transient long delistTimeSeconds;
    
    @Override
    public int getId() {
        return IdString.hashCode();
    }
    
    public int getStock(Player player) {
        // Not purchase count limit.
        if (this.Stock <= 0) {
            return GameConstants.UNLIMITED_STOCK;
        }

        return Math.max(Stock - player.getInventory().getMallShopPurchaseCount(this.getIdString()), 0);
    }

    /**
     * Returns whether the item is currently within its configured mall list window.
     */
    public boolean isVisible() {
        long now = Nebula.getCurrentServerTime();
        return (this.listTimeSeconds <= 0 || now >= this.listTimeSeconds)
                && (this.delistTimeSeconds <= 0 || now < this.delistTimeSeconds);
    }

    /**
     * Returns whether the player can currently purchase the requested quantity.
     */
    public boolean canPurchase(Player player, int quantity) {
        return quantity > 0 && this.isVisible() && this.getStock(player) >= quantity;
    }

    /**
     * Returns the next relevant stock refresh time for this mall shop entry in epoch seconds.
     */
    public long getNextRefreshTime() {
        return Utils.getNextResetTimeSeconds(this.RefreshType);
    }
    
    @Override
    public void onLoad() {
        this.products = new ItemParamMap();
        this.listTimeSeconds = Utils.dateToSeconds(this.ListTime);
        this.delistTimeSeconds = Utils.dateToSeconds(this.DeListTime);
        
        if (this.ItemId > 0) {
            this.products.add(this.ItemId, this.ItemQty);
        }
    }

}
