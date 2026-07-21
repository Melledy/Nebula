package emu.nebula.server.handlers;

import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.net.HandlerId;
import emu.nebula.net.GameSession;

@HandlerId(NetMsgId.trace_hunt_info_req)
public class HandlerTraceHuntInfoReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        // Encode and send
        return session.encodeMsg(NetMsgId.trace_hunt_info_succeed_ack, session.getPlayer().getTraceHuntManager().toProto());
    }

}
