package emu.nebula.server.handlers;

import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.ActivityTrekkerVersusApply.ActivityTrekkerVersusApplyReq;
import emu.nebula.net.HandlerId;
import emu.nebula.game.activity.type.TrekkerVersusActivity;
import emu.nebula.net.GameSession;

@HandlerId(NetMsgId.activity_trekker_versus_apply_req)
public class HandlerActivityTrekkerVersusApplyReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        // Parse request
        var req = ActivityTrekkerVersusApplyReq.parseFrom(message);
        
        // Get activity
        var activity = session.getPlayer().getActivityManager().getActivity(TrekkerVersusActivity.class, req.getActivityId());
        
        if (activity == null) {
            return session.encodeMsg(NetMsgId.activity_trekker_versus_apply_failed_ack);
        }
        
        // Apply
        boolean result = activity.apply(req.getBuild().getBuildId(), req.getAffixIds());
        
        if (result == false) {
            return session.encodeMsg(NetMsgId.activity_trekker_versus_apply_failed_ack);
        }
        
        // Encode and send
        return session.encodeMsg(NetMsgId.activity_trekker_versus_apply_succeed_ack);
    }

}
