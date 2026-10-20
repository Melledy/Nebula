package emu.nebula.server.handlers;

import emu.nebula.net.GameSession;
import emu.nebula.net.HandlerId;
import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.DormRewardReceive.DormRewardReceiveReq;

@HandlerId(NetMsgId.dorm_reward_receive_req)
public class HandlerDormRewardReceiveReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        // Parse request
        var req = DormRewardReceiveReq.parseFrom(message);

        // Receive rewards
        var change = session.getPlayer().getDormManager().receiveRewards(req.getRewardIds().toArray());

        if (change == null) {
            return session.encodeMsg(NetMsgId.dorm_reward_receive_failed_ack);
        }

        // Encode and send
        return session.encodeMsg(NetMsgId.dorm_reward_receive_succeed_ack, change.toProto());
    }

}
