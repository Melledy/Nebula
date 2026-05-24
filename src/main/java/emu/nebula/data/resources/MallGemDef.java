package emu.nebula.data.resources;

import com.google.gson.annotations.SerializedName;

import emu.nebula.data.BaseDef;
import emu.nebula.data.ResourceType;
import emu.nebula.game.inventory.ItemParamMap;
import emu.nebula.game.player.Player;
import emu.nebula.proto.MallGemListOuterClass.GemInfo;
import lombok.Getter;

@Getter
@ResourceType(name = "MallGem.json")
public class MallGemDef extends BaseDef {
    @SerializedName("Id")
    private String IdString;
    private int BaseItemId;
    private int BaseItemQty;
    private int ExperiencedBonusItemId;
    private int ExperiencedBonusItemQty;
    private int MaidenBonusItemID;
    private int MaidenBonusItemQty;
    private int Price;

    @Override
    public int getId() {
        return IdString.hashCode();
    }

    /**
     * Builds the actual delivery payload for the current player state.
     */
    public ItemParamMap buildProducts(Player player) {
        var products = new ItemParamMap();

        if (BaseItemId > 0 && BaseItemQty > 0) {
            products.add(BaseItemId, BaseItemQty);
        }

        if (this.hasMaidenBonus(player)) {
            if (MaidenBonusItemID > 0 && MaidenBonusItemQty > 0) {
                products.add(MaidenBonusItemID, MaidenBonusItemQty);
            }
            return products;
        }

        if (ExperiencedBonusItemId > 0 && ExperiencedBonusItemQty > 0) {
            products.add(ExperiencedBonusItemId, ExperiencedBonusItemQty);
        }

        return products;
    }

    public boolean hasMaidenBonus(Player player) {
        return player.getInventory().hasMallGemMaidenBonus(this.IdString);
    }

    public GemInfo toInfo(Player player) {
        return GemInfo.newInstance()
                .setId(this.getIdString())
                .setMaiden(this.hasMaidenBonus(player));
    }

}
