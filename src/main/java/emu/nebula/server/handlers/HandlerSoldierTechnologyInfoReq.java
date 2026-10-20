package emu.nebula.server.handlers;

import emu.nebula.net.GameSession;
import emu.nebula.net.HandlerId;
import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.Public.Bytes;

@HandlerId(NetMsgId.soldier_technology_info_req)
public class HandlerSoldierTechnologyInfoReq extends NetHandler {
    @Override
    public byte[] handle(GameSession session, byte[] message) {
        // Template response to get rid of errors
        var rsp = Bytes.newInstance();
        
        // Encode and send
        return session.encodeMsg(NetMsgId.soldier_technology_info_succeed_ack, rsp);
    }
}
