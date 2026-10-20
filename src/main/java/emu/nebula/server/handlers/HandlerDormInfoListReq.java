package emu.nebula.server.handlers;

import emu.nebula.net.GameSession;
import emu.nebula.net.HandlerId;
import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;

@HandlerId(NetMsgId.dorm_info_list_req)
public class HandlerDormInfoListReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        // Build response
        var rsp = session.getPlayer().getDormManager().toProto();

        // Encode and send
        return session.encodeMsg(NetMsgId.dorm_info_list_succeed_ack, rsp);
    }

}
