package emu.nebula.server.handlers;

import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.TraceHuntRecommend.TraceHuntRecommendResp;
import emu.nebula.net.HandlerId;
import emu.nebula.net.GameSession;

@HandlerId(NetMsgId.trace_hunt_recommend_req)
public class HandlerTraceHuntRecommendReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        // Build response
        var rsp = TraceHuntRecommendResp.newInstance();
        
        // TODO add other players
        
        // Encode and send
        return session.encodeMsg(NetMsgId.trace_hunt_recommend_succeed_ack, rsp);
    }

}
