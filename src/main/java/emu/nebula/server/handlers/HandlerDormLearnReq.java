package emu.nebula.server.handlers;

import emu.nebula.net.GameSession;
import emu.nebula.net.HandlerId;
import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;

@HandlerId(NetMsgId.dorm_learn_req)
public class HandlerDormLearnReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        // Dorm quests are not implemented yet, so acknowledge the request without changing state.
        return session.encodeMsg(NetMsgId.dorm_learn_succeed_ack);
    }

}
