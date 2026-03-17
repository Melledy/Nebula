package emu.nebula.server.handlers;

import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.ActivityPenguinCardLevelSettle.ActivityPenguinCardSettleReq;
import emu.nebula.net.HandlerId;
import emu.nebula.net.GameSession;

@HandlerId(NetMsgId.activity_penguin_card_level_settle_req)
public class HandlerActivityPenguinCardSettleReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        // Parse request proto
        var req = ActivityPenguinCardSettleReq.parseFrom(message);

        // Get activity

        // Encode and send
        return session.encodeMsg(NetMsgId.activity_penguin_card_level_settle_succeed_ack);
    }

}
