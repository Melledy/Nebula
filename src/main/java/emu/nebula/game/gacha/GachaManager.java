package emu.nebula.game.gacha;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import emu.nebula.Nebula;
import emu.nebula.data.resources.GachaDef;
import emu.nebula.data.resources.GachaNewbieDef;
import emu.nebula.database.GameDatabaseObject;
import emu.nebula.game.player.Player;
import emu.nebula.game.player.PlayerManager;
import it.unimi.dsi.fastutil.ints.IntList;
import lombok.Getter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Getter
@Entity(value = "gacha", useDiscriminator = false)
public class GachaManager extends PlayerManager implements GameDatabaseObject {
    private static final String PATH_BANNERS = "banners.";
    private static final String PATH_PITY_STATES = "pityStates.";
    private static final String PATH_NEWBIE_STATES = "newbieStates.";
    private static final int HISTORY_SAVE_RETRIES = 2;

    @Id
    private int uid;

    private Map<Integer, GachaBannerInfo> banners = new HashMap<>();
    private Map<Integer, GachaPityState> pityStates = new HashMap<>();
    private Map<Integer, NewbieGachaState> newbieStates = new HashMap<>();
    private transient Set<Integer> lockedNewbieObtainIds = new HashSet<>();

    @Deprecated // Morphia only
    public GachaManager() {
        
    }
    
    public GachaManager(Player player) {
        this();
        this.setPlayer(player);
        this.uid = player.getUid();

        this.save();
    }

    public synchronized NewbieObtainLock lockNewbieObtain(int newbieId) {
        if (this.lockedNewbieObtainIds.contains(newbieId)) {
            return null;
        }

        this.lockedNewbieObtainIds.add(newbieId);
        return new NewbieObtainLockHandle(newbieId);
    }

    public synchronized boolean isNewbieObtainLocked(int newbieId) {
        return this.lockedNewbieObtainIds.contains(newbieId);
    }

    private synchronized void unlockNewbieObtainInternal(int newbieId) {
        this.lockedNewbieObtainIds.remove(newbieId);
    }

    public synchronized GachaBannerInfo getBannerInfo(GachaDef gachaData) {
        return this.banners.computeIfAbsent(
                gachaData.getId(),
                i -> new GachaBannerInfo(gachaData)
        );
    }

    public synchronized GachaPityState getPityState(int storageId) {
        return this.pityStates.computeIfAbsent(storageId, i -> new GachaPityState());
    }

    public synchronized GachaBannerInfo findBannerInfo(int bannerId) {
        return this.banners.get(bannerId);
    }

    public synchronized GachaPityState findPityState(int storageId) {
        return this.pityStates.get(storageId);
    }

    public synchronized void saveSpinState(GachaBannerInfo info, int storageId, int gachaId, IntList results) {
        this.pityStates.computeIfAbsent(storageId, i -> new GachaPityState());

        var updates = new HashMap<String, Object>();
        updates.put(PATH_BANNERS + info.getId(), info);
        updates.put(PATH_PITY_STATES + storageId, this.pityStates.get(storageId));
        Nebula.getGameDatabase().update(this, this.getPlayerUid(), updates);

        var log = new GachaHistoryLog(this.getPlayerUid(), storageId, gachaId, results);
        Exception lastError = null;
        for (int attempt = 1; attempt <= HISTORY_SAVE_RETRIES + 1; attempt++) {
            try {
                Nebula.getGameDatabase().save(log);
                return;
            } catch (Exception e) {
                lastError = e;
            }
        }

        Nebula.getLogger().warn(
                "Failed to persist gacha history after retries. uid={}, type={}, gid={}, time={}",
                log.getPlayerUid(),
                log.getType(),
                log.getGid(),
                log.getTime(),
                lastError
        );
    }

    public synchronized NewbieGachaState getOrCreateNewbieState(GachaNewbieDef newbieDef) {
        var state = this.newbieStates.get(newbieDef.getId());
        if (state == null) {
            state = new NewbieGachaState(newbieDef.getId(), newbieDef.getSpinCount(), newbieDef.getSaveCount());
            this.newbieStates.put(newbieDef.getId(), state);
        }

        state.applyConfig(newbieDef.getSaveCount());
        return state;
    }

    public synchronized NewbieGachaState findNewbieState(int newbieId) {
        return this.newbieStates.get(newbieId);
    }

    public synchronized void saveNewbieState(NewbieGachaState state) {
        this.newbieStates.put(state.getId(), state);

        Nebula.getGameDatabase().update(
                this,
                this.getPlayerUid(),
                PATH_NEWBIE_STATES + state.getId(),
                state
        );
    }

    public void saveBanner(GachaBannerInfo info) {
        Nebula.getGameDatabase().update(
                this,
                this.getPlayerUid(),
                PATH_BANNERS + info.getId(),
                info
        );
    }

    private final class NewbieObtainLockHandle implements NewbieObtainLock {
        private final int newbieId;
        private boolean closed;

        private NewbieObtainLockHandle(int newbieId) {
            this.newbieId = newbieId;
        }

        @Override
        public void close() {
            synchronized (GachaManager.this) {
                if (this.closed) {
                    return;
                }

                this.closed = true;
                unlockNewbieObtainInternal(this.newbieId);
            }
        }
    }

    public interface NewbieObtainLock extends AutoCloseable {
        @Override
        void close();
    }

}
