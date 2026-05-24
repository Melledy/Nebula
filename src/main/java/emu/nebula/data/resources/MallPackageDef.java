package emu.nebula.data.resources;

import com.google.gson.annotations.SerializedName;
import emu.nebula.GameConstants;
import emu.nebula.data.BaseDef;
import emu.nebula.data.ResourceType;
import emu.nebula.Nebula;
import emu.nebula.game.inventory.ItemParamMap;
import emu.nebula.game.player.Player;
import emu.nebula.util.JsonUtils;
import emu.nebula.util.ResetCycle;
import emu.nebula.util.Utils;
import lombok.Getter;

@Getter
@ResourceType(name = "MallPackage.json")
public class MallPackageDef extends BaseDef {

    @SerializedName("Id")
    private String IdString;
    private int Stock;
    private int CurrencyType;
    private int CurrencyItemId;
    private int CurrencyItemQty;
    private int Tag;
    private String Items;
    private String ListTime;
    private String DeListTime;
    private int RefreshType;
    private int OrderCondType;
    private String OrderCondParams;
    private int ListCondType;
    private String ListCondParams;
    
    private transient ItemParamMap products;
    private transient long listTimeSeconds;
    private transient long delistTimeSeconds;
    private transient int[] orderCondParams;
    private transient int[] listCondParams;
    
    @Override
    public int getId() {
        return IdString.hashCode();
    }
    
    public int getStock(Player player) {
        return Math.max(Stock - player.getInventory().getMallPackagePurchaseCount(this.IdString), 0);
    }

    /**
     * Returns whether this package is a free mall package(CurrencyType = 3).
     */
    public boolean isFreePackage() {
        return this.CurrencyType == GameConstants.CURRENCY_TYPE_FREE;
    }

    /**
     * Returns whether this package is a cash mall package(CurrencyType = 1).
     */
    public boolean isCashPackage() {
        return this.CurrencyType == GameConstants.CURRENCY_TYPE_CASH;
    }

    /**
     * Returns whether this package is bought with Stellanite Lumina.
     */
    public boolean isItemPackage() {
        return this.CurrencyType == GameConstants.CURRENCY_TYPE_ITEM;
    }

    /**
     * Checks whether the player can purchase this package.
     */
    public boolean canPurchase(Player player) {
        return this.isVisible(player)
                && this.getStock(player) > 0
                && ShopCondition.matches(player, this.OrderCondType, this.orderCondParams);
    }

    /**
     * Returns whether this package should be included in the mall package list for the player.
     */
    public boolean isVisible(Player player) {
        long now = Nebula.getCurrentServerTime();
        if (!this.shouldIgnoreSaleWindow()) {
            if (this.listTimeSeconds > 0 && now < this.listTimeSeconds) {
                return false;
            }

            if (this.delistTimeSeconds > 0 && now >= this.delistTimeSeconds) {
                return false;
            }
        }

        return ShopCondition.matches(player, this.ListCondType, this.listCondParams);
    }

    /**
     * Display all skins via config
     */
    private boolean shouldIgnoreSaleWindow() {
        return this.Tag == GameConstants.TAG_SKIN && Nebula.getConfig().getServerOptions().isShowAllSkinInMall();
    }

    /**
     * Returns the next relevant stock refresh time for this package in epoch seconds.
     */
    public long getNextRefreshTime() {
        return Utils.getNextResetTimeSeconds(this.RefreshType);
    }

    @Override
    public void onLoad() {
        this.products = ItemParamMap.fromJsonString(this.Items);
        this.listTimeSeconds = Utils.dateToSeconds(this.ListTime);
        this.delistTimeSeconds = Utils.dateToSeconds(this.DeListTime);
        this.orderCondParams = JsonUtils.decode(this.OrderCondParams, int[].class);
        this.listCondParams = JsonUtils.decode(this.ListCondParams, int[].class);
    }

}
