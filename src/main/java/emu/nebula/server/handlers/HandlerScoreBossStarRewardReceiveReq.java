package emu.nebula.server.handlers;

import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.ScoreBossStarRewardReceive.ScoreBossStarRewardReceiveReq;
import emu.nebula.net.HandlerId;
import emu.nebula.net.GameSession;

@HandlerId(NetMsgId.score_boss_star_reward_receive_req)
public class HandlerScoreBossStarRewardReceiveReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        // Parse request
        var req = ScoreBossStarRewardReceiveReq.parseFrom(message);
        
        // Claim rewards
        var change = session.getPlayer().getScoreBossManager().claimRewards(req.getStar());
        
        // Encode and send
        return session.encodeMsg(NetMsgId.score_boss_star_reward_receive_succeed_ack, change.toProto());
    }

}
