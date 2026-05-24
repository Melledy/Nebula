package emu.nebula.game.mall;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import emu.nebula.Nebula;
import emu.nebula.data.resources.MallPackageDef;
import emu.nebula.data.resources.MallGemDef;
import emu.nebula.data.resources.MallMonthlyCardDef;
import emu.nebula.data.resources.BattlePassDef;
import emu.nebula.data.resources.DropPkgDef;
import emu.nebula.game.GameContext;
import emu.nebula.game.GameContextModule;
import emu.nebula.game.inventory.ItemParamMap;
import emu.nebula.game.player.PlayerChangeInfo;
import emu.nebula.net.GameSession;
import emu.nebula.net.NetMsgId;
import emu.nebula.net.PacketHelper;
import emu.nebula.proto.MallGemOrder.OrderInfo;
import emu.nebula.proto.Public.HeadIcon;
import emu.nebula.proto.Notify.OrderStateChange;
import emu.nebula.proto.Public.Item;
import emu.nebula.proto.Public.ChangeInfo;
import emu.nebula.proto.Public.Res;
import emu.nebula.util.JsonUtils;

/**
 * Mock payment, isolate mock order data
 */
public class PayModule extends GameContextModule {

    public PayModule(GameContext context) {
        super(context);
    }

    @FunctionalInterface
    private interface CollectSettlement {
        MallOrderCollectResult resolve(GameSession session) throws Exception;
    }

    private final ConcurrentHashMap<Integer, CollectSettlement> pendingCollects = new ConcurrentHashMap<>();
    private volatile Map<Integer, int[]> battlePassDropPackages;

    public void clearPendingCollect(int uid) {
        pendingCollects.remove(uid);
    }

    /**
     * Resolves the authoritative collect payload and clears the transient order state.
     */
    public MallOrderCollectResult consumePendingCollect(GameSession session) throws Exception {
        CollectSettlement settlement = pendingCollects.remove(session.getPlayer().getUid());
        if (settlement == null) {
            return null;
        }

        return settlement.resolve(session);
    }

    /**
     * Keeps mock-order flows aligned with normal inventory updates by reusing the
     * same item notify packets as the rest of the server.
     */
    public void pushInventoryNotifies(GameSession session, ChangeInfo items) {
        if (items == null || items.isEmpty()) {
            return;
        }

        session.getPlayer().addNextPackage(NetMsgId.items_change_notify, items);
    }

    /**
     * Use mock payment for gem recharge to match official mall order flow
     */
    public OrderInfo createGemOrder(GameSession session, MallGemDef data) {
        return this.createOrder(session, "gem", data.getIdString(), collectSession -> {
            var change = collectSession.getPlayer().getInventory().buyMallGem(data);
            if (change == null) {
                return MallOrderCollectResult.empty();
            }

            return MallOrderCollectResult.of(change.toProto());
        });
    }

    /**
     * Defer monthly card settlement to maintain consistent mock-payment lifecycle with mall flow.
     */
    public OrderInfo createMonthlyCardOrder(GameSession session, MallMonthlyCardDef data) {
        var player = session.getPlayer();
        if (data == null || !data.canPurchase(player)) {
            return null;
        }

        int durationDays = data.getMonthlyCardDuration();
        return this.createOrder(session, "monthly", data.getIdString(), collectSession -> {
            var collectPlayer = collectSession.getPlayer();
            var collectChange = collectPlayer.getInventory().buyMallMonthlyCard(data);
            if (collectChange == null) {
                return MallOrderCollectResult.empty();
            }

            collectPlayer.activateMonthlyCard(data.getIdString(), durationDays);
            var dailyRewardChange = collectPlayer.createMonthlyCardRewardChange(data.getIdString());
            if (dailyRewardChange != null) {
                collectChange.add(dailyRewardChange);
            }

            return MallOrderCollectResult.of(collectChange.toProto());
        });
    }

    /**
     * Cash mall packages follow the same order -> paid notify -> collect lifecycle as other RMB products.
     */
    public OrderInfo createPackageOrder(GameSession session, MallPackageDef data) {
        if (data == null || !data.isCashPackage() || !data.canPurchase(session.getPlayer())) {
            return null;
        }

        return this.createOrder(session, "package", data.getIdString(), collectSession -> {
            var change = collectSession.getPlayer().getInventory().buyMallPackage(data);
            if (change == null) {
                return MallOrderCollectResult.empty();
            }

            return MallOrderCollectResult.of(change.toProto());
        });
    }

    /**
     * Create mock battle pass order following standard payment flow:
     * order confirm -> payment notify -> reward settlement.
     */
    public OrderInfo createBattlePassOrder(GameSession session, BattlePassDef data, int requestedMode) {
        return this.createOrder(session, "battlepass", data.getId() + ":" + requestedMode, collectSession -> {
            var player = collectSession.getPlayer();
            var battlePass = player.getBattlePassManager().getBattlePass();
            if (battlePass == null) {
                return MallOrderCollectResult.empty();
            }

            int currentMode = battlePass.getMode();
            if (requestedMode <= currentMode) {
                return MallOrderCollectResult.empty();
            }

            battlePass.setMode(requestedMode);
            if (requestedMode == 2 && data.getLuxuryBonusLevel() > 0) {
                battlePass.setLevel(battlePass.getLevel() + data.getLuxuryBonusLevel());
            }

            var rewards = new ItemParamMap();

            int collectItemId = 0;
            int collectItemQty = 0;

            // BattlePass.json semantics:
            // - mode=1 (Premium) unlocks premium claim lane only, no immediate collect reward.
            // - mode=2 (Luxury) grants immediate luxury reward.
            // - mode=1 -> mode=2 upgrade uses complementary reward config.
            if (requestedMode == 2) {
                if (currentMode == 0) {
                    collectItemId = data.getLuxuryTid();
                    collectItemQty = data.getLuxuryQty();
                } else if (currentMode == 1) {
                    collectItemId = data.getComplementaryTid();
                    collectItemQty = data.getComplementaryQty();
                }
            }

            this.addBattlePassCollectRewards(rewards, collectItemId, collectItemQty);

            battlePass.save();

            if (rewards.isEmpty()) {
                return MallOrderCollectResult.empty();
            }

            var change = player.getInventory().addItems(rewards);
            if (change == null || change.isEmpty()) {
                return MallOrderCollectResult.empty();
            }

            var stateChange = change.toProto();
            var displayChange = this.buildBattlePassDisplayChange(change);
            return MallOrderCollectResult.split(stateChange, displayChange);
        });
    }

    /**
     * Resolves battle-pass collect rewards.
     * <p>
     * Some battle-pass collect ids in Item.json are intermediate "drop keys" that
     * should expand to concrete rewards (for example, skin/head icon), otherwise
     * the client misses skin-gain specific reward presentation.
     */
    private void addBattlePassCollectRewards(ItemParamMap rewards, int collectItemId, int collectItemQty) {
        if (collectItemId <= 0 || collectItemQty <= 0) {
            return;
        }

        var packageIds = this.getBattlePassDropPackages().get(collectItemId);
        if (packageIds == null || packageIds.length == 0) {
            rewards.add(collectItemId, collectItemQty);
            return;
        }

        for (int i = 0; i < collectItemQty; i++) {
            for (int packageId : packageIds) {
                int dropItemId = DropPkgDef.getRandomDrop(packageId);
                if (dropItemId > 0) {
                    rewards.add(dropItemId, 1);
                }
            }
        }
    }

    /**
     * Loads battle-pass drop-package mapping from Drop.json on first use and caches it.
     * Mapping rule: DropId -> [PkgId...].
     */
    private Map<Integer, int[]> getBattlePassDropPackages() {
        var cache = this.battlePassDropPackages;
        if (cache != null) {
            return cache;
        }

        synchronized (this) {
            cache = this.battlePassDropPackages;
            if (cache != null) {
                return cache;
            }

            cache = this.loadBattlePassDropPackages();
            this.battlePassDropPackages = cache;
            return cache;
        }
    }

    private Map<Integer, int[]> loadBattlePassDropPackages() {
        final String dropPath = Nebula.getConfig().resourceDir + "/bin/Drop.json";
        var result = new LinkedHashMap<Integer, int[]>();

        try {
            var rows = JsonUtils.loadToMap(dropPath, String.class, DropRow.class);
            if (rows == null || rows.isEmpty()) {
                return Collections.emptyMap();
            }

            var aggregate = new LinkedHashMap<Integer, List<Integer>>();
            for (var row : rows.values()) {
                if (row == null || row.DropId <= 0 || row.PkgId <= 0) {
                    continue;
                }
                aggregate.computeIfAbsent(row.DropId, ignored -> new ArrayList<>()).add(row.PkgId);
            }

            for (var entry : aggregate.entrySet()) {
                var values = entry.getValue();
                if (values == null || values.isEmpty()) {
                    continue;
                }
                int[] packageIds = new int[values.size()];
                for (int i = 0; i < values.size(); i++) {
                    packageIds[i] = values.get(i);
                }
                result.put(entry.getKey(), packageIds);
            }
        } catch (Exception exception) {
            Nebula.getLogger().error("Failed to load battle-pass drop mapping from {}", dropPath, exception);
            return Collections.emptyMap();
        }

        return result;
    }

    /**
     * Minimal Drop.json row shape needed for battle-pass mapping.
     */
    private static final class DropRow {
        private int DropId;
        private int PkgId;
    }

    /**
     * Battle-pass collect triggers 3 client presentations:
     * skin animation (notify queue), normal reward popup (CollectResp.Items), level refresh (main response).
     * <p>
     * Only popup-safe items are shown in reward popup to avoid duplicate skin/head display.
     * Avatar rewards are converted to item format for the client's general reward popup.
     */
    private ChangeInfo buildBattlePassDisplayChange(PlayerChangeInfo change) {
        if (change == null || change.isEmpty()) {
            return ChangeInfo.newInstance();
        }

        var display = new PlayerChangeInfo();
        for (var any : change.getList()) {
            String typeUrl = any.getTypeUrl();
            if (typeUrl.endsWith(Item.class.getSimpleName()) || typeUrl.endsWith(Res.class.getSimpleName())) {
                display.getList().add(any.clone());
                continue;
            }

            if (typeUrl.endsWith(HeadIcon.class.getSimpleName())) {
                try {
                    var headIcon = HeadIcon.parseFrom(any.getValue().toArray());
                    display.add(Item.newInstance().setTid(headIcon.getTid()).setQty(1));
                } catch (Exception ignored) {
                    // ignore
                }
            }
        }

        return display.toProto();
    }

    private OrderInfo createOrder(GameSession session, String orderType, String payloadKey, CollectSettlement settlement) {
        int uid = session.getPlayer().getUid();
        String orderId = orderType + "." + uid + "." + System.currentTimeMillis();
        this.pendingCollects.put(uid, settlement);
        return this.buildMockOrder(orderId, orderType + ":" + payloadKey + ":" + orderId);
    }

    private OrderInfo buildMockOrder(String orderId, String extraData) {
        var paidNotify = OrderStateChange.newInstance()
                .setOrderId(orderId)
                .setStore(orderId.startsWith("battlepass.") ? 3 : 1);

        return OrderInfo.newInstance()
                .setId(orderId)
                .setExtraData(extraData)
                .setNotifyUrl(String.format("http://localhost:%s/mock-pay", Nebula.getConfig().getHttpServer().getBindPort()))
                .setNextPackage(PacketHelper.encodeMsg(NetMsgId.order_paid_notify, paidNotify));
    }

}
