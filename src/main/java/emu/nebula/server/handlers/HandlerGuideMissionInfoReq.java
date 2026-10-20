package emu.nebula.server.handlers;

import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.GuideMissionInfoOuterClass.GuideMissionInfo;
import emu.nebula.net.HandlerId;
import emu.nebula.net.GameSession;

@HandlerId(NetMsgId.guide_mission_info_req)
public class HandlerGuideMissionInfoReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        // Build response
        var rsp = GuideMissionInfo.newInstance();
        
        // Encode and send
        return session.encodeMsg(NetMsgId.guide_mission_info_succeed_ack, rsp);
    }

}
