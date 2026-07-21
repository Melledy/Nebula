package emu.nebula.server.handlers;

import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.TraceHuntBossRewardReceive.TraceHuntBossRewardReceiveResp;
import emu.nebula.net.HandlerId;
import emu.nebula.net.GameSession;

@HandlerId(NetMsgId.trace_hunt_boss_reward_receive_req)
public class HandlerTraceHuntBossRewardReceiveReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        // Get manager
        var manager = session.getPlayer().getTraceHuntManager();
        
        // Claim reward
        var change = manager.claimReward();
        
        if (change == null) {
            return session.encodeMsg(NetMsgId.trace_hunt_boss_reward_receive_failed_ack);
        }
        
        // Build response
        var rsp = TraceHuntBossRewardReceiveResp.newInstance()
                .setLevel(manager.getLevel())
                .setExp(manager.getExp())
                .setChangeInfo(change.toProto());
        
        // Encode and send
        return session.encodeMsg(NetMsgId.trace_hunt_boss_reward_receive_succeed_ack, rsp);
    }

}
