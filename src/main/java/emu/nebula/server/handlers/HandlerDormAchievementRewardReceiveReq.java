package emu.nebula.server.handlers;

import emu.nebula.net.GameSession;
import emu.nebula.net.HandlerId;
import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.DormAchievementRewardReceive.DormAchievementRewardReceiveReq;

@HandlerId(NetMsgId.dorm_achievement_reward_receive_req)
public class HandlerDormAchievementRewardReceiveReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        // Parse request
        var req = DormAchievementRewardReceiveReq.parseFrom(message);

        // Receive achievements
        var rsp = session.getPlayer().getDormManager().receiveAchievements(req.getAchievementIds().toArray());

        if (rsp == null) {
            return session.encodeMsg(NetMsgId.dorm_achievement_reward_receive_failed_ack);
        }

        // Encode and send
        return session.encodeMsg(NetMsgId.dorm_achievement_reward_receive_succeed_ack, rsp);
    }

}
