package emu.nebula.server.handlers;

import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.Public.UI32;
import emu.nebula.net.HandlerId;
import emu.nebula.game.activity.type.LoginRewardActivity;
import emu.nebula.net.GameSession;

@HandlerId(NetMsgId.activity_login_reward_receive_req)
public class HandlerActivityLoginRewardReceiveReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        // Parse request
        var req = UI32.parseFrom(message);
        
        // Get activity id from request
        int activityId = req.getValue();
        
        // Get activity
        // Note: Sign-in activities are handled by the same request as the login reward activities
        var activity = session.getPlayer().getActivityManager().getActivity(LoginRewardActivity.class, activityId);
        
        if (activity == null) {
            return session.encodeMsg(NetMsgId.activity_login_reward_receive_failed_ack);
        }
        
        // Claim any rewards
        var change = activity.claim();
        
        if (change == null) {
            return session.encodeMsg(NetMsgId.activity_login_reward_receive_failed_ack);
        }
        
        // Encode and send
        return session.encodeMsg(NetMsgId.activity_login_reward_receive_succeed_ack, change.toProto());
    }

}
