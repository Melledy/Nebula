package emu.nebula.server.handlers;

import emu.nebula.game.activity.type.SoldierActivity;
import emu.nebula.net.GameSession;
import emu.nebula.net.HandlerId;
import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.SoldierStepOut.SoldierStepOutReq;

@HandlerId(NetMsgId.soldier_step_out_req)
public class HandlerSoldierStepOutReq extends NetHandler {
    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        var req = SoldierStepOutReq.parseFrom(message);
        var activity = session.getPlayer().getActivityManager().getFirstActivity(SoldierActivity.class);
        if (activity == null || !req.hasDeploy() || !activity.stepOut(req.getDeploy())) {
            return session.encodeMsg(NetMsgId.soldier_step_out_failed_ack);
        }
        return session.encodeMsg(NetMsgId.soldier_step_out_succeed_ack);
    }
}
