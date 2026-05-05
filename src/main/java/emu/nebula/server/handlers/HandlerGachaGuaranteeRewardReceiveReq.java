package emu.nebula.server.handlers;

import emu.nebula.Nebula;
import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.Public.UI32;
import emu.nebula.net.HandlerId;
import emu.nebula.net.GameSession;

@HandlerId(NetMsgId.gacha_guarantee_reward_receive_req)
public class HandlerGachaGuaranteeRewardReceiveReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        // Parse req
        var req = UI32.parseFrom(message);
        
        // Receive guaranteed reward
        var change = Nebula.getGameContext().getGachaModule().recvGuarantee(session.getPlayer(), req.getValue());
        
        if (change == null) {
            return session.encodeMsg(NetMsgId.gacha_guarantee_reward_receive_failed_ack);
        }
        
        // Encode and send
        return session.encodeMsg(NetMsgId.gacha_guarantee_reward_receive_succeed_ack, change.toProto());
    }

}
