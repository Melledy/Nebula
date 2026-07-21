package emu.nebula.server.handlers;

import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.TraceHuntApply.TraceHuntApplyReq;
import emu.nebula.net.HandlerId;
import emu.nebula.net.GameSession;

@HandlerId(NetMsgId.trace_hunt_apply_req)
public class HandlerTraceHuntApplyReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        // Parse Request
        var req = TraceHuntApplyReq.parseFrom(message);
        
        // Hunt
        boolean success = session.getPlayer().getTraceHuntManager().apply(req.getOwnerUID(), req.getBossID(), req.getBuildID());
        
        if (!success) {
            return session.encodeMsg(NetMsgId.trace_hunt_apply_failed_ack);
        }
        
        // Encode and send
        return session.encodeMsg(NetMsgId.trace_hunt_apply_succeed_ack);
    }

}
