package emu.nebula.server.handlers;

import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.TraceHuntSettle.TraceHuntSettleReq;
import emu.nebula.proto.TraceHuntSettle.TraceHuntSettleResp;
import emu.nebula.net.HandlerId;

import java.util.List;

import emu.nebula.game.tracehunt.TraceHuntLog;
import emu.nebula.net.GameSession;

@HandlerId(NetMsgId.trace_hunt_settle_req)
public class HandlerTraceHuntSettleReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        // Parse request
        var req = TraceHuntSettleReq.parseFrom(message);
        
        // Get manager
        var manager = session.getPlayer().getTraceHuntManager();
        
        // Hunt
        var change = manager.settle(req.getScore(), req.getStar());
        
        if (change == null) {
            return session.encodeMsg(NetMsgId.trace_hunt_settle_failed_ack);
        }
        
        // Handle client events for achievements
        session.getPlayer().getAchievementManager().handleClientEvents(req.getEvents());
        
        // Build response
        var rsp = TraceHuntSettleResp.newInstance()
                .setLevel(manager.getLevel())
                .setExp(manager.getExp())
                .setSelfHuntTimes(manager.getHuntCount())
                .setBossCreateTime(manager.getBossCreateTime());
        
        @SuppressWarnings("unchecked")
        var logs = (List<TraceHuntLog>) change.getExtraData();
        for (var log : logs) {
            rsp.addHuntLog(log.toProto());
        }
        
        // Encode and send
        return session.encodeMsg(NetMsgId.trace_hunt_settle_succeed_ack, rsp);
    }

}
