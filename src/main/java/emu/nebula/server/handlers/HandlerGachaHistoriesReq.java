package emu.nebula.server.handlers;

import emu.nebula.Nebula;
import emu.nebula.data.GameData;
import emu.nebula.net.GameSession;
import emu.nebula.net.HandlerId;
import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.GachaHistoriesOuterClass.GachaHistories;
import emu.nebula.proto.Public.UI32;

@HandlerId(NetMsgId.gacha_histories_req)
public class HandlerGachaHistoriesReq extends NetHandler {
    private static final int MAX_HISTORY_SIZE = 2000;
    // Return histories from the recent 6 months only
    private static final long HISTORY_RETENTION_SECONDS = 60L * 60L * 24L * 30L * 6L;

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        // Parse request
        var req = UI32.parseFrom(message);
        int requestedStorageId = req.getValue();
        if (requestedStorageId <= 0 || GameData.getGachaStorageDataTable().get(requestedStorageId) == null) {
            return session.encodeMsg(NetMsgId.gacha_histories_failed_ack);
        }

        long retentionStartTime = Nebula.getCurrentServerTime() - HISTORY_RETENTION_SECONDS;

        var rsp = GachaHistories.newInstance();
        var logs = Nebula.getGameDatabase().getSortedObjects(
                emu.nebula.game.gacha.GachaHistoryLog.class,
                "playerUid",
                session.getPlayer().getUid(),
                "type",
                requestedStorageId,
                "time",
                retentionStartTime,
                "time",
                MAX_HISTORY_SIZE
        );
        java.util.Collections.reverse(logs);
        for (var log : logs) {
            rsp.addList(log.toProto());
        }

        // Encode and send
        return session.encodeMsg(NetMsgId.gacha_histories_succeed_ack, rsp);
    }

}
