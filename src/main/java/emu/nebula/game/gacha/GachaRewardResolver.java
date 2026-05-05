package emu.nebula.game.gacha;

import emu.nebula.data.GameData;
import emu.nebula.game.inventory.ItemAcquireMap;
import emu.nebula.game.inventory.ItemParamMap;
import emu.nebula.game.inventory.ItemType;
import emu.nebula.game.player.Player;
import emu.nebula.game.player.PlayerChangeInfo;
import emu.nebula.proto.Public.Transform;
import it.unimi.dsi.fastutil.ints.IntList;

import java.util.function.IntPredicate;

public final class GachaRewardResolver {
    private static final int MAX_DISC_COUNT = 6;
    private static final int ITEM_ID_TRAVEL_PERMIT = 23;
    private static final int ITEM_ID_EXPERT_PERMIT = 24;
    private static final int EXPERT_PERMIT_PER_DUP_CHAR = 40;
    private static final int TRAVEL_PERMIT_PER_DISC = 100;

    private GachaRewardResolver() {
    }

    public static boolean isResolvable(IntList cards) {
        if (cards == null || cards.isEmpty()) {
            return false;
        }

        return isResolvable(consumer -> {
            for (int id : cards) {
                if (!consumer.test(id)) {
                    return false;
                }
            }
            return true;
        });
    }

    public static boolean isResolvable(int[] cards) {
        if (cards == null || cards.length == 0) {
            return false;
        }

        return isResolvable(consumer -> {
            for (int id : cards) {
                if (!consumer.test(id)) {
                    return false;
                }
            }
            return true;
        });
    }

    private static boolean isResolvable(IntPredicateRunner runner) {
        return runner.run(GachaRewardResolver::isResolvableItem);
    }

    private static boolean isResolvableItem(int itemId) {
        var itemData = GameData.getItemDataTable().get(itemId);
        if (itemData == null) {
            return false;
        }

        if (itemData.getItemType() == ItemType.Char) {
            var characterData = GameData.getCharacterDataTable().get(itemId);
            return characterData != null;
        }

        if (itemData.getItemType() == ItemType.Disc) {
            var discData = GameData.getDiscDataTable().get(itemId);
            return discData != null;
        }

        return true;
    }

    public static RewardPlan resolve(ItemAcquireMap acquireItems, ItemParamMap baseBonusItems) {
        if (acquireItems == null) {
            return null;
        }

        var grantItems = new ItemParamMap();
        if (baseBonusItems != null) {
            grantItems.add(baseBonusItems);
        }

        var transformSrcItems = new ItemParamMap();
        var transformDstItems = new ItemParamMap();

        int characters = 0;

        for (var entry : acquireItems.getItems().int2ObjectEntrySet()) {
            int id = entry.getIntKey();
            var acquire = entry.getValue();

            if (acquire.getType() == ItemType.Char) {
                int count = acquire.getCount();
                int newCharacterCount = acquire.getBegin() == 0 ? 1 : 0;
                int duplicateCount = Math.max(count - newCharacterCount, 0);

                if (duplicateCount > 0) {
                    var characterData = GameData.getCharacterDataTable().get(id);
                    if (characterData == null) {
                        continue;
                    }

                    transformSrcItems.add(id, duplicateCount);
                    transformDstItems.add(characterData.getFragmentsId(), characterData.getTransformQty() * duplicateCount);
                    transformDstItems.add(ITEM_ID_EXPERT_PERMIT, EXPERT_PERMIT_PER_DUP_CHAR * duplicateCount);
                }

                if (newCharacterCount > 0) {
                    grantItems.add(id, newCharacterCount);
                }
                characters += acquire.getCount();
            } else if (acquire.getType() == ItemType.Disc) {
                int begin = acquire.getBegin();
                int count = acquire.getCount();

                int newDiscCount = begin == 0 ? 1 : 0;
                int duplicateCount = Math.max(count - newDiscCount, 0);
                int effectiveBegin = begin + newDiscCount;

                int maxTransformCount = Math.max(MAX_DISC_COUNT - effectiveBegin, 0);
                int transformCount = Math.min(duplicateCount, maxTransformCount);
                int extraCount = Math.max(duplicateCount - transformCount, 0);

                if (transformCount > 0) {
                    var discData = GameData.getDiscDataTable().get(id);
                    if (discData == null) {
                        continue;
                    }

                    transformSrcItems.add(id, transformCount);
                    transformDstItems.add(discData.getTransformItemId(), transformCount);
                }
                if (extraCount > 0) {
                    transformSrcItems.add(id, extraCount);
                    transformDstItems.add(ITEM_ID_TRAVEL_PERMIT, TRAVEL_PERMIT_PER_DISC * extraCount);
                }

                if (newDiscCount > 0) {
                    grantItems.add(id, newDiscCount);
                }
                grantItems.add(ITEM_ID_TRAVEL_PERMIT, TRAVEL_PERMIT_PER_DISC * acquire.getCount());
            } else {
                grantItems.add(id, acquire.getCount());
            }
        }

        grantItems.add(transformDstItems);
        return new RewardPlan(
                acquireItems,
                grantItems,
                new TransformLog(transformSrcItems, transformDstItems),
                characters
        );
    }

    public static int apply(Player player, RewardPlan plan, PlayerChangeInfo change) {
        if (player == null || plan == null || change == null) {
            return 0;
        }

        player.getInventory().addItems(plan.grantItems(), change);
        change.add(plan.acquireItems().toProto());

        var transform = Transform.newInstance();
        plan.transformLog().srcItems().toItemTemplateStream().forEach(transform::addSrc);
        plan.transformLog().dstItems().toItemTemplateStream().forEach(transform::addDst);
        change.add(transform);

        return plan.characterCount();
    }

    @FunctionalInterface
    private interface IntPredicateRunner {
        boolean run(IntPredicate predicate);
    }

    public record RewardPlan(
            ItemAcquireMap acquireItems,
            ItemParamMap grantItems,
            TransformLog transformLog,
            int characterCount
    ) {
    }

    public record TransformLog(
            ItemParamMap srcItems,
            ItemParamMap dstItems
    ) {
    }
}
