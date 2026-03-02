package emu.nebula.server.handlers;

import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.net.HandlerId;
import emu.nebula.net.GameSession;

@HandlerId(NetMsgId.daily_mall_reward_receive_req)
public class HandlerDailyMallRewardReceiveReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        // Claim daily reward
        var change = session.getPlayer().getQuestManager().claimDailyMallGift();
        
        if (change == null) {
            return session.encodeMsg(NetMsgId.daily_mall_reward_receive_failed_ack);
        }
        
        // Encode and send
        return session.encodeMsg(NetMsgId.daily_mall_reward_receive_succeed_ack, change.toProto());
    }

}
