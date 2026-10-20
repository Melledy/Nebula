package emu.nebula.server.handlers;

import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.Public.UI32;
import emu.nebula.net.HandlerId;
import emu.nebula.net.GameSession;

@HandlerId(NetMsgId.quest_tower_reward_receive_req)
public class HandlerQuestTowerRewardReceiveReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        // Parse request
        var req = UI32.parseFrom(message);
        
        // Receive rewards
        var change = session.getPlayer().getQuestManager().receiveStarTowerQuestReward(req.getValue());
        
        if (change == null) {
            return session.encodeMsg(NetMsgId.quest_tower_reward_receive_failed_ack);
        }
        
        // Encode and send
        return session.encodeMsg(NetMsgId.quest_tower_reward_receive_succeed_ack, change.toProto());
    }

}
