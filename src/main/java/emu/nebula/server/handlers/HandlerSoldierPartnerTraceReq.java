package emu.nebula.server.handlers;

import emu.nebula.game.activity.type.SoldierActivity;
import emu.nebula.net.GameSession;
import emu.nebula.net.HandlerId;
import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.SoldierPartnerTrace.SoldierPartnerTraceReq;

@HandlerId(NetMsgId.soldier_partner_trace_req)
public class HandlerSoldierPartnerTraceReq extends NetHandler {
    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        var req = SoldierPartnerTraceReq.parseFrom(message);
        var activity = session.getPlayer().getActivityManager().getFirstActivity(SoldierActivity.class);
        if (activity == null || !activity.setPartnerTrace(req.getPartnerType(), req.getTrace())) {
            return session.encodeMsg(NetMsgId.soldier_partner_trace_failed_ack);
        }
        return session.encodeMsg(NetMsgId.soldier_partner_trace_succeed_ack);
    }
}
