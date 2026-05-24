package emu.nebula.data.resources;

import com.google.gson.annotations.SerializedName;

import emu.nebula.data.BaseDef;
import emu.nebula.data.GameData;
import emu.nebula.data.ResourceType;
import emu.nebula.game.inventory.ItemParamMap;
import emu.nebula.game.player.Player;
import emu.nebula.proto.MallMonthlycardList.MonthlyCardInfo;
import lombok.Getter;

@Getter
@ResourceType(name = "MallMonthlyCard.json")
public class MallMonthlyCardDef extends BaseDef {
    @SerializedName("Id")
    private String IdString;
    private int MonthlyCardId;
    private int Price;
    private int BaseItemId;
    private int BaseItemQty;
    private int MaxDays;
    
    private transient ItemParamMap products;
    private transient ItemParamMap dailyRewards;
    
    @Override
    public int getId() {
        return IdString.hashCode();
    }

    /**
     * Monthly card valid duration(days).
     */
    public int getMonthlyCardDuration() {
        return 30;
    }

    /**
     * Monthly cards can be repurchased until the remaining duration exceeds the configured cap.
     */
    public boolean canPurchase(Player player) {
        return this.getRemainingDays(player) <= this.MaxDays;
    }

    /**
     * Returns the player's current remaining duration for this monthly card.
     */
    public int getRemainingDays(Player player) {
        return player.getMonthlyCardRemainingDays(this.getIdString());
    }

    /**
     * Returns whether today's monthly-card reward has already been claimed.
     */
    public boolean hasReceivedRewardToday(Player player) {
        return player.receivedMonthlyCardRewardToday(this.getIdString());
    }

    public ItemParamMap getProducts() {
        if (products == null) {
            products = new ItemParamMap();
            
            // Add base items (initial purchase reward)
            if (BaseItemId > 0 && BaseItemQty > 0) {
                products.add(BaseItemId, BaseItemQty);
            }
        }
        
        return products;
    }

    /**
     * Returns the configured daily login rewards for this monthly card.
     */
    public ItemParamMap getDailyRewards() {
        if (this.dailyRewards == null) {
            this.dailyRewards = new ItemParamMap();

            for (var rewardData : GameData.getMonthlyCardRewardDataTable()) {
                if (rewardData.getCardId() != this.MonthlyCardId) {
                    continue;
                }
                this.dailyRewards.add(rewardData.getRewards());
                break;
            }
        }

        return this.dailyRewards.clone();
    }

    public MonthlyCardInfo toInfo(Player player) {
        return MonthlyCardInfo.newInstance()
                .setId(this.getIdString())
                .setRemaining(this.getRemainingDays(player))
                .setReceived(this.hasReceivedRewardToday(player));
    }

}
