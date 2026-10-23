package emu.nebula.server.handlers;

import emu.nebula.Nebula;
import emu.nebula.net.GameSession;
import emu.nebula.net.HandlerId;
import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.Public.UI32;

@HandlerId(NetMsgId.gacha_reward_get_req)
public class HandlerGachaRewardGetReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        // Parse req
        var req = UI32.parseFrom(message);
        
        // Receive reward
        var change = Nebula.getGameContext().getGachaModule().recvReward(session.getPlayer(), req.getValue());
        
        if (change == null) {
            return session.encodeMsg(NetMsgId.gacha_reward_get_failed_ack);
        }
        
        // Encode and send
        return session.encodeMsg(NetMsgId.gacha_reward_get_succeed_ack, change.toProto());
    }

}
