package emu.nebula.server.handlers;

import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.ActivityStoryApply.ActivityStoryApplyReq;
import emu.nebula.net.HandlerId;
import emu.nebula.game.activity.type.StoryActivity;
import emu.nebula.net.GameSession;

@HandlerId(NetMsgId.activity_story_apply_req)
public class HandlerActivityStoryApplyReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        // Parse req
        var req = ActivityStoryApplyReq.parseFrom(message);
        
        // Get activity
        var activity = session.getPlayer().getActivityManager().getActivity(StoryActivity.class, req.getActivityId());
        
        if (activity == null) {
            return session.encodeMsg(NetMsgId.activity_story_apply_failed_ack);
        }
        
        // Apply
        boolean success = activity.apply(req.getStoryId(), req.getBuildId());

        if (success == false) {
            return session.encodeMsg(NetMsgId.activity_story_apply_failed_ack);
        }
        
        // Encode and send
        return session.encodeMsg(NetMsgId.activity_story_apply_succeed_ack);
    }

}
