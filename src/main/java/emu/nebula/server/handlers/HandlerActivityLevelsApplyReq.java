package emu.nebula.server.handlers;

import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.ActivityLevelsApply.ActivityLevelsApplyReq;
import emu.nebula.net.HandlerId;
import emu.nebula.game.activity.type.LevelsActivity;
import emu.nebula.net.GameSession;

@HandlerId(NetMsgId.activity_levels_apply_req)
public class HandlerActivityLevelsApplyReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        // Parse req
        var req = ActivityLevelsApplyReq.parseFrom(message);
        
        // Get activity
        var activity = session.getPlayer().getActivityManager().getActivity(LevelsActivity.class, req.getActivityId());
        
        if (activity == null) {
            return session.encodeMsg(NetMsgId.activity_levels_apply_failed_ack);
        }
        
        // Apply
        boolean success = activity.apply(req.getLevelId(), req.getBuild().getBuildId());

        if (success == false) {
            return session.encodeMsg(NetMsgId.activity_levels_apply_failed_ack);
        }
        
        // Encode and send
        return session.encodeMsg(NetMsgId.activity_levels_apply_succeed_ack);
    }

}
