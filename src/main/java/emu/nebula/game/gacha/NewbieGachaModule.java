package emu.nebula.game.gacha;

import emu.nebula.data.GameData;
import emu.nebula.data.resources.GachaNewbieDef;
import emu.nebula.game.inventory.ItemAcquireMap;
import emu.nebula.game.player.Player;
import emu.nebula.game.player.PlayerChangeInfo;
import emu.nebula.proto.GachaNewbieInfoOuterClass.GachaNewbieInfo;
import emu.nebula.proto.GachaNewbieInfoOuterClass.UI32s;
import it.unimi.dsi.fastutil.ints.IntArrayList;

import java.util.ArrayList;
import java.util.List;

public final class NewbieGachaModule {

    public List<GachaNewbieInfo> listInfos(Player player) {
        var newbieDefs = GameData.getGachaNewbieDataTable().values();
        var infos = new ArrayList<GachaNewbieInfo>(newbieDefs.size());
        var manager = player.getGachaManager();

        synchronized (manager) {
            for (var data : newbieDefs) {
                var state = manager.getOrCreateNewbieState(data);
                boolean received = state.isReceived();
                int usedSpinCount = Math.max(0, data.getSpinCount() - state.getRemainingSpinCount());

                var info = GachaNewbieInfo.newInstance()
                        .setId(data.getId())
                        .setTimes(usedSpinCount)
                        .setReceive(received);

                if (!received) {
                    var pendingResult = state.getPendingResult();
                    if (pendingResult != null) {
                        info.getMutableTemp().addAllValues(pendingResult);
                    }

                    for (var cards : state.getSavedResults()) {
                        if (cards != null && cards.length > 0) {
                            info.addCards(UI32s.newInstance().addAllValues(cards));
                        }
                    }
                }

                infos.add(info);
            }
        }

        return infos;
    }

    public int[] spin(Player player, int newbieId) {
        GachaNewbieDef newbieDef = GameData.getGachaNewbieDataTable().get(newbieId);
        if (newbieDef == null) {
            return null;
        }

        int newbieStateId = newbieDef.getId();
        var bannerDef = GameData.getGachaDataTable().get(newbieStateId);
        if (bannerDef == null) {
            return null;
        }

        synchronized (player.getGachaManager()) {
            NewbieGachaState state = player.getGachaManager().getOrCreateNewbieState(newbieDef);
            if (player.getGachaManager().isNewbieObtainLocked(newbieStateId) || !state.canSpin()) {
                return null;
            }

            int[] cards = NewbieRollStrategy.rollTenPull(bannerDef, NewbieRollStrategy.defaultProfile());
            if (!GachaRewardResolver.isResolvable(cards)) {
                return null;
            }

            if (!state.applySpinResult(cards)) {
                return null;
            }

            player.getGachaManager().saveNewbieState(state);
            return cards;
        }
    }

    public boolean save(Player player, int newbieId, Integer index) {
        var newbieDef = GameData.getGachaNewbieDataTable().get(newbieId);
        if (newbieDef == null) {
            return false;
        }

        synchronized (player.getGachaManager()) {
            if (player.getGachaManager().isNewbieObtainLocked(newbieId)) {
                return false;
            }
            var state = player.getGachaManager().getOrCreateNewbieState(newbieDef);
            if (state == null || !state.canSavePendingResult()) {
                return false;
            }

            if (!state.savePendingResult(index)) {
                return false;
            }

            player.getGachaManager().saveNewbieState(state);
            return true;
        }
    }

    public PlayerChangeInfo obtain(Player player, int newbieId, int index) {
        GachaNewbieDef gachaNewbieDef = GameData.getGachaNewbieDataTable().get(newbieId);
        if (gachaNewbieDef == null) {
            return null;
        }

        int newbieStateId = gachaNewbieDef.getId();
        if (!player.getGachaManager().tryLockNewbieObtain(newbieStateId)) {
            return null;
        }

        try {
            NewbieGachaState state;
            int[] cards;
            synchronized (player.getGachaManager()) {
                state = player.getGachaManager().findNewbieState(newbieStateId);
                if (state == null || !state.canObtain(index)) {
                    return null;
                }

                cards = state.copySavedResult(index);
                if (!GachaRewardResolver.isResolvable(cards)) {
                    return null;
                }
            }

            var change = new PlayerChangeInfo();
            var acquireItems = new ItemAcquireMap(player, new IntArrayList(cards));
            var rewardPlan = GachaRewardResolver.resolve(acquireItems, null);
            if (rewardPlan == null) {
                return null;
            }

            synchronized (player.getGachaManager()) {
                if (!state.markReceived(index)) {
                    return null;
                }

                player.getGachaManager().saveNewbieState(state);
            }

            GachaRewardResolver.apply(player, rewardPlan, change);
            return change;
        } finally {
            player.getGachaManager().unlockNewbieObtain(newbieStateId);
        }
    }

}
