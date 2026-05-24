package emu.nebula.server.handlers;

import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.BattlePassQuestRewardReceive.BattlePassQuestRewardResp;
import emu.nebula.proto.Public.UI32;
import emu.nebula.net.HandlerId;
import emu.nebula.net.GameSession;

@HandlerId(NetMsgId.battle_pass_quest_reward_receive_req)
public class HandlerBattlePassQuestRewardReceiveReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        if (!session.getPlayer().isBattlePassUnlocked()) {
            return session.encodeMsg(NetMsgId.battle_pass_quest_reward_receive_failed_ack);
        }

        // Parse req
        var req = UI32.parseFrom(message);
        
        // Recieve reward
        var battlePass = session.getPlayer().getBattlePassManager().getBattlePass().receiveQuestReward(req.getValue());
        
        if (battlePass == null) {
            return session.encodeMsg(NetMsgId.battle_pass_quest_reward_receive_failed_ack);
        }
        
        // Build response
        var rsp = BattlePassQuestRewardResp.newInstance()
                .setLevel(battlePass.getLevel())
                .setExp(battlePass.getExp())
                .setExpThisWeek(battlePass.getExpWeek());

        session.getPlayer().queueBattlePassStateNotify();
        
        // Encode and send
        return session.encodeMsg(NetMsgId.battle_pass_quest_reward_receive_succeed_ack, rsp);
    }

}
