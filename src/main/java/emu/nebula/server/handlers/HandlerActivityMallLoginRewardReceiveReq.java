package emu.nebula.server.handlers;

import emu.nebula.game.activity.type.MallLoginActivity;
import emu.nebula.net.GameSession;
import emu.nebula.net.HandlerId;
import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.Public.UI32;

@HandlerId(NetMsgId.activity_malllogin_reward_receive_req)
public class HandlerActivityMallLoginRewardReceiveReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        // Parse request
        var req = UI32.parseFrom(message);
        
        // Get activity
        var activity = session.getPlayer().getActivityManager().getActivity(MallLoginActivity.class, req.getValue());
        
        if (activity == null) {
            return session.encodeMsg(NetMsgId.activity_malllogin_reward_receive_failed_ack);
        }
        
        // Claim any rewards
        var change = activity.claim();
        
        if (change == null) {
            return session.encodeMsg(NetMsgId.activity_malllogin_reward_receive_failed_ack);
        }
        
        // Encode and send
        return session.encodeMsg(NetMsgId.activity_malllogin_reward_receive_succeed_ack, change.toProto());
    }

}
