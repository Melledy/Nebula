package emu.nebula.server.handlers;

import emu.nebula.game.activity.type.SoldierActivity;
import emu.nebula.net.GameSession;
import emu.nebula.net.HandlerId;
import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.SoldierApply.SoldierApplyReq;

@HandlerId(NetMsgId.soldier_apply_req)
public class HandlerSoldierApplyReq extends NetHandler {
    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        var req = SoldierApplyReq.parseFrom(message);
        var activity = session.getPlayer().getActivityManager().getActivity(
                SoldierActivity.class, req.getSeasonId());
        if (activity == null) {
            return session.encodeMsg(NetMsgId.soldier_apply_failed_ack);
        }

        var rsp = activity.apply(req.getSeasonId(), req.getGradeChallengeId());
        return rsp == null
                ? session.encodeMsg(NetMsgId.soldier_apply_failed_ack)
                : session.encodeMsg(NetMsgId.soldier_apply_succeed_ack, rsp);
    }
}
