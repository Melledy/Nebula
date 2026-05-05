package emu.nebula.game.gacha;

import emu.nebula.Nebula;
import emu.nebula.data.GameData;
import emu.nebula.data.resources.GachaStorageDef;
import emu.nebula.game.GameContext;
import emu.nebula.game.GameContextModule;
import emu.nebula.game.achievement.AchievementCondition;
import emu.nebula.game.inventory.ItemAcquireMap;
import emu.nebula.game.inventory.ItemParamMap;
import emu.nebula.game.player.Player;
import emu.nebula.game.player.PlayerChangeInfo;
import emu.nebula.game.gacha.GachaBannerInfo.GachaBannerDraft;
import emu.nebula.game.gacha.GachaPityState.GachaPityDraft;
import emu.nebula.proto.GachaNewbieInfoOuterClass.GachaNewbieInfo;
import it.unimi.dsi.fastutil.ints.IntArrayList;

import java.util.List;

public class GachaModule extends GameContextModule {
    private final NewbieGachaModule newbieGachaModule = new NewbieGachaModule();

    public GachaModule(GameContext context) {
        super(context);
    }

    public GachaResult spin(Player player, int bannerId, int amount) {
        var data = GameData.getGachaDataTable().get(bannerId);
        if (data == null || !data.isActiveAt(Nebula.getCurrentServerTime())) {
            return null;
        }

        var storage = data.getStorageData();

        var manager = player.getGachaManager();
        synchronized (manager) {
            var info = manager.getBannerInfo(data);
            var pityState = manager.getPityState(data.getStorageId());
            var spinPlan = prepareSpin(player, data, storage, amount, info, pityState);
            if (spinPlan == null) {
                return null;
            }

            var change = applySpin(player, storage, spinPlan);
            persistSpin(manager, info, pityState, data, spinPlan);

            player.trigger(AchievementCondition.GachaTotal, amount);
            player.trigger(AchievementCondition.GachaCharacterTotal, spinPlan.rewardPlan().characterCount());

            return new GachaResult(info, spinPlan.pityDraft(), change, spinPlan.cards());
        }
    }

    public PlayerChangeInfo recvGuarantee(Player player, int bannerId) {
        var manager = player.getGachaManager();
        synchronized (manager) {
            var info = manager.findBannerInfo(bannerId);
            if (info == null) {
                return null;
            }

            var data = GameData.getGachaDataTable().get(bannerId);
            if (data == null) {
                return null;
            }

            if (!data.isActiveAt(Nebula.getCurrentServerTime())) {
                return null;
            }

            if (!data.canGuarantee() || info.getTotal() < data.getGuaranteeTimes()) {
                return null;
            }

            if (info.isUsedGuarantee()) {
                return null;
            }
            info.setUsedGuarantee(true);

            manager.saveBanner(info);
            return player.getInventory().addItem(data.getGuaranteeTid(), data.getGuaranteeQty());
        }
    }

    public List<GachaNewbieInfo> listNewbieInfos(Player player) {
        return this.newbieGachaModule.listInfos(player);
    }

    public int[] spinNewbie(Player player, int newbieId) {
        return this.newbieGachaModule.spin(player, newbieId);
    }

    public boolean saveNewbie(Player player, int newbieId, Integer index) {
        return this.newbieGachaModule.save(player, newbieId, index);
    }

    public PlayerChangeInfo obtainNewbie(Player player, int newbieId, int index) {
        return this.newbieGachaModule.obtain(player, newbieId, index);
    }

    private BonusItemsOutcome buildBonusItems(GachaStorageDef storage,
                                              int firstTenMultiplier,
                                              int amount,
                                              GachaBannerDraft snapshot) {
        var bonusItems = new ItemParamMap();
        var giveItemsMap = storage.getGiveItemsMap();
        if (giveItemsMap == null || giveItemsMap.isEmpty()) {
            return new BonusItemsOutcome(bonusItems, snapshot);
        }

        int multiplier = 1;
        var draft = snapshot;
        if (amount == GachaMode.TEN.getAmount() && !snapshot.usedFirstTen()) {
            multiplier = Math.max(1, firstTenMultiplier);
            draft = new GachaBannerDraft(snapshot.total(), true, snapshot.usedGuarantee());
        }

        for (var entry : giveItemsMap.entries()) {
            bonusItems.add(entry.getIntKey(), entry.getIntValue() * multiplier * amount);
        }
        return new BonusItemsOutcome(bonusItems, draft);
    }

    private record BonusItemsOutcome(
            ItemParamMap bonusItems,
            GachaBannerDraft bannerDraft
    ) {
    }

    private SpinPlan prepareSpin(Player player,
                                 emu.nebula.data.resources.GachaDef data,
                                 GachaStorageDef storage,
                                 int amount,
                                 GachaBannerInfo info,
                                 GachaPityState pityState) {
        var inventory = player.getInventory();
        int specificConsumeQty = resolveSpecificConsumeQty(inventory, data, amount);
        int coveredPullCount = specificConsumeQty > 0 && data.getSpecificQty() > 0
                ? specificConsumeQty / data.getSpecificQty()
                : 0;
        int remainingDefaultCostReq = storage.getDefaultQty() * Math.max(amount - coveredPullCount, 0);
        int defaultQty = inventory.getItemCount(storage.getDefaultId());
        int convertReq = 0;
        if (remainingDefaultCostReq > defaultQty) {
            int convertQty = inventory.getResourceCount(storage.getCostId());
            convertReq = storage.getCostQty() * (remainingDefaultCostReq - defaultQty);
            if (convertReq > convertQty) {
                return null;
            }
        }
        int consumeDefaultQty = Math.min(remainingDefaultCostReq, defaultQty);

        var pityDraft = pityState.copyForSpin();
        var bannerDraft = info.copyForSpin();
        var bonusOutcome = buildBonusItems(storage, data.getFirstTenShow(), amount, bannerDraft);
        bannerDraft = bonusOutcome.bannerDraft();

        var cards = new IntArrayList(amount);
        for (int i = 0; i < amount; i++) {
            var pullOutcome = GachaRollEngine.pull(data, pityDraft);
            if (pullOutcome == null || pullOutcome.itemId() <= 0) {
                Nebula.getLogger().warn("Gacha roll produced invalid item. uid={}, bannerId={}", player.getUid(), data.getId());
                return null;
            }

            pityDraft = pullOutcome.pityDraft();
            cards.add(pullOutcome.itemId());
        }

        bannerDraft = new GachaBannerDraft(
                bannerDraft.total() + amount,
                bannerDraft.usedFirstTen(),
                bannerDraft.usedGuarantee()
        );

        if (!GachaRewardResolver.isResolvable(cards)) {
            Nebula.getLogger().warn("Gacha roll contains unresolved rewards. uid={}, bannerId={}", player.getUid(), data.getId());
            return null;
        }

        var acquireItems = new ItemAcquireMap(player, cards);
        var rewardPlan = GachaRewardResolver.resolve(acquireItems, bonusOutcome.bonusItems());
        if (rewardPlan == null) {
            return null;
        }

        return new SpinPlan(
                bannerDraft,
                pityDraft,
                cards,
                rewardPlan,
                new CostPlan(data.getSpecificTid(), specificConsumeQty, convertReq, consumeDefaultQty)
        );
    }

    private PlayerChangeInfo applySpin(Player player, GachaStorageDef storage, SpinPlan spinPlan) {
        var change = new PlayerChangeInfo();
        var inventory = player.getInventory();

        if (spinPlan.costPlan().consumeSpecificQty() > 0) {
            inventory.removeItem(spinPlan.costPlan().specificItemId(), spinPlan.costPlan().consumeSpecificQty(), change);
        }
        if (spinPlan.costPlan().convertReq() > 0) {
            inventory.removeItem(storage.getCostId(), spinPlan.costPlan().convertReq(), change);
        }
        if (spinPlan.costPlan().consumeDefaultQty() > 0) {
            inventory.removeItem(storage.getDefaultId(), spinPlan.costPlan().consumeDefaultQty(), change);
        }

        GachaRewardResolver.apply(player, spinPlan.rewardPlan(), change);
        return change;
    }

    private void persistSpin(GachaManager manager,
                             GachaBannerInfo info,
                             GachaPityState pityState,
                             emu.nebula.data.resources.GachaDef data,
                             SpinPlan spinPlan) {
        info.overwriteFrom(spinPlan.bannerDraft());
        pityState.overwriteFrom(spinPlan.pityDraft());
        manager.saveSpinState(info, data.getStorageId(), data.getId(), spinPlan.cards());
    }

    private int resolveSpecificConsumeQty(emu.nebula.game.inventory.Inventory inventory,
                                          emu.nebula.data.resources.GachaDef data,
                                          int amount) {
        if (data.getSpecificTid() <= 0 || data.getSpecificQty() <= 0) {
            return 0;
        }

        int specificReq = data.getSpecificQty() * amount;
        int specificQty = inventory.getItemCount(data.getSpecificTid());
        return Math.min(specificReq, specificQty);
    }

    private record CostPlan(
            int specificItemId,
            int consumeSpecificQty,
            int convertReq,
            int consumeDefaultQty
    ) {
    }

    private record SpinPlan(
            GachaBannerDraft bannerDraft,
            GachaPityDraft pityDraft,
            IntArrayList cards,
            GachaRewardResolver.RewardPlan rewardPlan,
            CostPlan costPlan
    ) {
    }

}
