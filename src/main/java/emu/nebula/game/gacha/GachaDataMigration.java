package emu.nebula.game.gacha;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import emu.nebula.data.GameData;
import emu.nebula.util.JsonUtils;
import org.bson.Document;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;

import emu.nebula.Nebula;

public final class GachaDataMigration {
    private static final String TABLE_GACHA = "gacha";
    private static final String TABLE_GACHA_HISTORY = "gacha_history";

    private static final String FIELD_ID = "_id";
    private static final String FIELD_PITY_STATES = "pityStates";
    private static final String FIELD_HISTORIES = "histories";

    private static final String FIELD_PLAYER_UID = "playerUid";
    private static final String FIELD_TYPE = "type";
    private static final String FIELD_GID = "gid";
    private static final String FIELD_TIME = "time";
    private static final String FIELD_IDS = "ids";

    private static final String FIELD_MISS_TIMES_A = "missTimesA";
    private static final String FIELD_MISS_TIMES_UP_A = "missTimesUpA";
    private static final String FIELD_MISS_TIMES_B = "missTimesB";

    @SuppressWarnings({"MismatchedQueryAndUpdateOfCollection"})
    private static final class LegacyPlayerGachaDoc {
        int _id;
        Map<Integer, LegacyBannerState> banners;
        Map<Integer, List<LegacyHistoryRow>> histories;
    }

    private static final class LegacyBannerState {
        int id;
        int missTimesA;
        int missTimesUpA;
        int missTimesB;
    }

    private static final class LegacyHistoryRow {
        int type;
        int gid;
        long time;
        List<Integer> ids = new ArrayList<>();
    }

    private static Document derivePityStatesFromBanners(Map<Integer, LegacyBannerState> banners) {
        if (banners == null || banners.isEmpty()) {
            return null;
        }

        var pityStates = new Document();
        for (var entry : banners.entrySet()) {
            var bannerState = entry.getValue();
            if (bannerState == null) {
                continue;
            }

            // Trust inner banner id; map key is treated as legacy container key.
            int bannerId = bannerState.id;
            if (bannerId <= 0) {
                continue;
            }

            var data = GameData.getGachaDataTable().get(bannerId);
            if (data == null) {
                continue;
            }

            // New pity state key is storageId, so multiple banners may merge here.
            String storageKey = String.valueOf(data.getStorageId());
            var existing = pityStates.get(storageKey, Document.class);
            int existingA = existing != null ? existing.getInteger(FIELD_MISS_TIMES_A, 0) : 0;
            int existingUpA = existing != null ? existing.getInteger(FIELD_MISS_TIMES_UP_A, 0) : 0;
            int existingB = existing != null ? existing.getInteger(FIELD_MISS_TIMES_B, 0) : 0;

            // Keep the max counters per storageId to preserve the strongest pity progress.
            int missTimesA = Math.max(existingA, bannerState.missTimesA);
            int missTimesUpA = Math.max(existingUpA, bannerState.missTimesUpA);
            int missTimesB = Math.max(existingB, bannerState.missTimesB);

            pityStates.put(storageKey, new Document(FIELD_MISS_TIMES_A, missTimesA)
                    .append(FIELD_MISS_TIMES_UP_A, missTimesUpA)
                    .append(FIELD_MISS_TIMES_B, missTimesB));
        }

        return pityStates.isEmpty() ? null : pityStates;
    }

    // One-time startup migration for legacy gacha data.
    // - Derive pityStates from banner-embedded pity counters.
    // - Move embedded histories into a single table: gacha_history.
    public static void run() {
        // Collections: source player gacha state + target split history table.
        var database = Nebula.getGameDatabase().getDatabase();
        var gachaCollection = database.getCollection(TABLE_GACHA);
        var historyCollection = database.getCollection(TABLE_GACHA_HISTORY);

        // Query index for history browsing by player and pool type.
        historyCollection.createIndex(new Document(FIELD_PLAYER_UID, 1).append(FIELD_TYPE, 1).append(FIELD_TIME, -1));

        long scanned = 0;
        long updated = 0;
        long migratedHistoryAttempts = 0;
        long migratedHistoryInserts = 0;

        var migrationFilter = Filters.or(
                Filters.exists(FIELD_HISTORIES),
                Filters.and(Filters.exists("banners"), Filters.not(Filters.exists(FIELD_PITY_STATES)))
        );

        // Process only players that still have legacy fields to migrate.
        for (var gachaDoc : gachaCollection.find(migrationFilter)) {
            scanned++;

            // Step 1: decode raw document into typed legacy DTO.
            var legacy = JsonUtils.decode(gachaDoc.toJson(), LegacyPlayerGachaDoc.class);
            if (legacy == null || legacy._id <= 0) {
                continue;
            }

            int uid = legacy._id;
            var ops = new ArrayList<org.bson.conversions.Bson>();

            Object pityStates = gachaDoc.get(FIELD_PITY_STATES);
            if (pityStates == null) {
                // Step 2: derive storage-level pity states from legacy banners.
                var derivedPityStates = derivePityStatesFromBanners(legacy.banners);
                if (derivedPityStates != null) {
                    ops.add(Updates.set(FIELD_PITY_STATES, derivedPityStates));
                }
            }

            if (legacy.histories != null && !legacy.histories.isEmpty()) {
                var upserts = new ArrayList<UpdateOneModel<Document>>();
                for (var typeEntry : legacy.histories.entrySet()) {
                    var historyRows = typeEntry.getValue();
                    if (historyRows == null) {
                        continue;
                    }

                    for (var row : historyRows) {
                        if (row == null || row.ids == null) {
                            continue;
                        }
                        int type = row.type;
                        var filter = Filters.and(
                                Filters.eq(FIELD_PLAYER_UID, uid),
                                Filters.eq(FIELD_TYPE, type),
                                Filters.eq(FIELD_GID, row.gid),
                                Filters.eq(FIELD_TIME, row.time),
                                Filters.eq(FIELD_IDS, row.ids)
                        );

                        // Step 3: split one legacy history row into gacha_history (idempotent upsert).
                        upserts.add(new UpdateOneModel<>(
                                filter,
                                Updates.combine(
                                        Updates.setOnInsert(FIELD_PLAYER_UID, uid),
                                        Updates.setOnInsert(FIELD_TYPE, type),
                                        Updates.setOnInsert(FIELD_GID, row.gid),
                                        Updates.setOnInsert(FIELD_TIME, row.time),
                                        Updates.setOnInsert(FIELD_IDS, row.ids)
                                ),
                                new UpdateOptions().upsert(true)
                        ));
                    }
                }

                if (!upserts.isEmpty()) {
                    var result = historyCollection.bulkWrite(upserts);
                    migratedHistoryAttempts += upserts.size();
                    migratedHistoryInserts += result.getUpserts().size();
                }

                // Step 4: remove embedded legacy histories after split migration.
                ops.add(Updates.unset(FIELD_HISTORIES));
            }

            if (!ops.isEmpty()) {
                // Step 5: commit player-level migration changes.
                gachaCollection.updateOne(Filters.eq(FIELD_ID, uid), Updates.combine(ops));
                updated++;
            }
        }

        Nebula.getLogger().info(
                "Gacha migration completed. scanned={}, updated={}, historyAttempts={}, historyInserts={}",
                scanned,
                updated,
                migratedHistoryAttempts,
                migratedHistoryInserts
        );
    }
}
