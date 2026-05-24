package emu.nebula.server.handlers;

import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.BattlePassRewardReceive.BattlePassRewardReceiveReq;
import emu.nebula.proto.BattlePassRewardReceive.BattlePassRewardReceiveResp;
import emu.nebula.net.HandlerId;
import emu.nebula.game.player.PlayerChangeInfo;
import emu.nebula.net.GameSession;

@HandlerId(NetMsgId.battle_pass_reward_receive_req)
public class HandlerBattlePassRewardReceiveReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        if (!session.getPlayer().isBattlePassUnlocked()) {
            return session.encodeMsg(NetMsgId.battle_pass_reward_receive_failed_ack);
        }

        // Parse request
        var req = BattlePassRewardReceiveReq.parseFrom(message);
        
        // Setup variables
        PlayerChangeInfo change = null;
        var battlePass = session.getPlayer().getBattlePassManager().getBattlePass();
        
        // Claim
        if (req.getPremium() > 0) {
            change = session.getPlayer().getBattlePassManager().getBattlePass().receiveReward(true, req.getPremium());
        } else if (req.getBasic() > 0) {
            change = session.getPlayer().getBattlePassManager().getBattlePass().receiveReward(false, req.getBasic());
        } else if (req.hasAll()) {
            change = session.getPlayer().getBattlePassManager().getBattlePass().receiveReward();
        }
        
        // Check
        if (change == null) {
            return session.encodeMsg(NetMsgId.battle_pass_reward_receive_failed_ack);
        }
        
        // Build response
        var rsp = BattlePassRewardReceiveResp.newInstance()
                .setBasicReward(battlePass.getBasicReward().toByteArray())
                .setPremiumReward(battlePass.getPremiumReward().toByteArray())
                .setChange(change.toProto());

        session.getPlayer().queueBattlePassStateNotify();
        
        // Encode and send
        return session.encodeMsg(NetMsgId.battle_pass_reward_receive_succeed_ack, rsp);
    }

}
