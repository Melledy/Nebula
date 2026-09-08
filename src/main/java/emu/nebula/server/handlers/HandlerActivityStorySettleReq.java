package emu.nebula.server.handlers;

import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.ActivityStorySettleOuterClass.ActivityStorySettleReq;
import emu.nebula.net.HandlerId;
import emu.nebula.game.activity.type.StoryActivity;
import emu.nebula.net.GameSession;

@HandlerId(NetMsgId.activity_story_settle_req)
public class HandlerActivityStorySettleReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        // Parse req
        var req = ActivityStorySettleReq.parseFrom(message);
        
        // Get activity
        var activity = session.getPlayer().getActivityManager().getActivity(StoryActivity.class, req.getActivityId());
        
        if (activity == null) {
            return session.encodeMsg(NetMsgId.activity_story_settle_failed_ack);
        }
        
        // Apply
        var change = activity.settle(req.getMutableList(), req.getMutableEvidences());

        if (change == null) {
            return session.encodeMsg(NetMsgId.activity_story_settle_failed_ack);
        }
        
        // Handle client events for achievements
        session.getPlayer().getAchievementManager().handleClientEvents(req.getEvents());
        
        // Encode and send
        return session.encodeMsg(NetMsgId.activity_story_settle_succeed_ack, change.toProto());
    }

}
