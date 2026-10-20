package emu.nebula.server.handlers;

import emu.nebula.net.GameSession;
import emu.nebula.net.HandlerId;
import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.DormLayoutRewardReceive.DormLayoutRewardReceiveReq;

@HandlerId(NetMsgId.dorm_layout_reward_receive_req)
public class HandlerDormLayoutRewardReceiveReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        // Parse request
        var req = DormLayoutRewardReceiveReq.parseFrom(message);

        // Receive layout rewards
        var change = session.getPlayer().getDormManager().receiveLayoutRewards(req.getLayoutIds().toArray());

        if (change == null) {
            return session.encodeMsg(NetMsgId.dorm_layout_reward_receive_failed_ack);
        }

        // Encode and send
        return session.encodeMsg(NetMsgId.dorm_layout_reward_receive_succeed_ack, change.toProto());
    }

}
