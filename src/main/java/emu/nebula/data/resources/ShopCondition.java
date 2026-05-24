package emu.nebula.data.resources;

import emu.nebula.data.GameData;
import emu.nebula.game.player.Player;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.Getter;

public enum ShopCondition {
    None(0),
    WorldClassSpecific(71),
    ShopPreGoodsSellOut(85),
    ActivityShopPreGoodsSellOut(115);

    @Getter
    private final int value;
    private static final Int2ObjectMap<ShopCondition> map = new Int2ObjectOpenHashMap<>();

    static {
        for (ShopCondition type : ShopCondition.values()) {
            map.put(type.getValue(), type);
        }
    }

    ShopCondition(int value) {
        this.value = value;
    }

    public static ShopCondition getByValue(int value) {
        return map.get(value);
    }

    public static boolean matches(Player player, int condType, int[] condParams) {
        ShopCondition condition = ShopCondition.getByValue(condType);
        if (condition == ShopCondition.None) {
            return true;
        }
        if (condition == null) {
            return false;
        }

        if (condition == ShopCondition.WorldClassSpecific) {
            int requiredLevel = condParams != null && condParams.length > 0 ? condParams[0] : 0;
            return player.getLevel() >= requiredLevel;
        }

        return false;
    }

    public static boolean matchesResidentGoodsAppear(Player player, int condType, int[] condParams) {
        ShopCondition condition = ShopCondition.getByValue(condType);
        if (condition == ShopCondition.None) {
            return true;
        }
        if (condition == null) {
            return false;
        }

        if (condition == ShopCondition.ShopPreGoodsSellOut) {
            if (condParams == null || condParams.length < 2) {
                return false;
            }

            int requiredGoodsId = condParams[1];
            ResidentGoodsDef requiredGoods = GameData.getResidentGoodsDataTable().get(requiredGoodsId);
            if (requiredGoods == null) {
                return false;
            }

            int boughtCount = player.getInventory().getShopBuyCount().get(requiredGoodsId);
            return boughtCount > 0 && boughtCount >= requiredGoods.getMaximumLimit();
        }

        return false;
    }

}
