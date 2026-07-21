package emu.nebula.server.handlers;

import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.Public.I32;
import emu.nebula.proto.TraceHuntTrace.TraceHuntTraceResp;
import emu.nebula.net.HandlerId;

import java.util.List;

import emu.nebula.game.tracehunt.TraceHuntLog;
import emu.nebula.net.GameSession;

@HandlerId(NetMsgId.trace_hunt_trace_req)
public class HandlerTraceHuntTraceReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        // Parse request
        var req = I32.parseFrom(message);
        
        // Trace
        var change = session.getPlayer().getTraceHuntManager().trace(req.getValue());
        
        // Sanity check
        if (change == null) {
            return session.encodeMsg(NetMsgId.trace_hunt_trace_failed_ack);
        }
        
        // Build response
        var rsp = TraceHuntTraceResp.newInstance()
                .setBossID(session.getPlayer().getTraceHuntManager().getBossId())
                .setChangeInfo(change.toProto());
        
        @SuppressWarnings("unchecked")
        var logs = (List<TraceHuntLog>) change.getExtraData();
        for (var log : logs) {
            rsp.addTraceLog(log.toProto());
        }
        
        // Encode and send
        return session.encodeMsg(NetMsgId.trace_hunt_trace_succeed_ack, rsp);
    }

}
