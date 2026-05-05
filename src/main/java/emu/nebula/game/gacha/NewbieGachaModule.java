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

    private record NewbieRequest(GachaManager manager, GachaNewbieDef newbieDef) {
    }

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
        var request = resolveRequest(player, newbieId);
        if (request == null) {
            return null;
        }

        int newbieStateId = request.newbieDef().getId();
        var bannerDef = GameData.getGachaDataTable().get(newbieStateId);
        if (bannerDef == null) {
            return null;
        }

        var manager = request.manager();
        synchronized (manager) {
            var state = loadStateForSpin(manager, request.newbieDef());
            if (state == null) {
                return null;
            }

            int[] cards = NewbieRollStrategy.rollTenPull(bannerDef, NewbieRollStrategy.defaultProfile());
            if (!GachaRewardResolver.isResolvable(cards)) {
                return null;
            }

            if (!state.applySpinResult(cards)) {
                return null;
            }

            manager.saveNewbieState(state);
            return cards;
        }
    }

    public boolean save(Player player, int newbieId, Integer index) {
        int resolvedIndex = index == null ? -1 : index;
        var request = resolveRequest(player, newbieId);
        if (request == null) {
            return false;
        }

        var manager = request.manager();
        synchronized (manager) {
            var state = loadStateForSave(manager, request.newbieDef());
            if (state == null) {
                return false;
            }

            if (!state.savePendingResult(resolvedIndex)) {
                return false;
            }

            manager.saveNewbieState(state);
            return true;
        }
    }

    public PlayerChangeInfo obtain(Player player, int newbieId, int index) {
        if (index < 0) {
            return null;
        }

        var request = resolveRequest(player, newbieId);
        if (request == null) {
            return null;
        }

        int newbieStateId = request.newbieDef().getId();
        var obtainLock = request.manager().lockNewbieObtain(newbieStateId);
        if (obtainLock == null) {
            return null;
        }

        try (var ignored = obtainLock) {
            NewbieGachaState state;
            int[] cards;
            synchronized (request.manager()) {
                state = loadStateForObtain(request.manager(), request.newbieDef(), index);
                if (state == null) {
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

            synchronized (request.manager()) {
                if (!state.markReceived(index)) {
                    return null;
                }

                request.manager().saveNewbieState(state);
            }

            GachaRewardResolver.apply(player, rewardPlan, change);
            return change;
        }
    }

    private NewbieRequest resolveRequest(Player player, int newbieId) {
        var newbieDef = GameData.getGachaNewbieDataTable().get(newbieId);
        if (newbieDef == null) {
            return null;
        }

        return new NewbieRequest(player.getGachaManager(), newbieDef);
    }

    private NewbieGachaState loadStateForSpin(GachaManager manager, GachaNewbieDef newbieDef) {
        int newbieStateId = newbieDef.getId();
        if (manager.isNewbieObtainLocked(newbieStateId)) {
            return null;
        }
        var state = manager.getOrCreateNewbieState(newbieDef);
        if (!state.canSpin(false)) {
            return null;
        }

        return state;
    }

    private NewbieGachaState loadStateForSave(GachaManager manager,
                                              GachaNewbieDef newbieDef) {
        int newbieStateId = newbieDef.getId();
        if (manager.isNewbieObtainLocked(newbieStateId)) {
            return null;
        }
        var state = manager.getOrCreateNewbieState(newbieDef);
        if (!state.canSavePendingResult(false)) {
            return null;
        }

        return state;
    }

    private NewbieGachaState loadStateForObtain(GachaManager manager, GachaNewbieDef newbieDef, int index) {
        int newbieStateId = newbieDef.getId();
        var state = manager.findNewbieState(newbieStateId);
        if (state == null) {
            return null;
        }
        if (!state.canObtain(index)) {
            return null;
        }

        return state;
    }

}
