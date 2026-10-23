package emu.nebula.server.handlers;

import emu.nebula.net.GameSession;
import emu.nebula.net.HandlerId;
import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.Public.UI32V;
import emu.nebula.proto.QuestDormRewardRecevie.QuestDormRewardReceiveSucceedAck;

@HandlerId(NetMsgId.quest_dorm_reward_receive_req)
public class HandlerQuestDormRewardReceiveReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        // Parse request
        var req = UI32V.parseFrom(message);

        // Receive rewards
        var dorm = session.getPlayer().getDormManager();
        var change = dorm.receiveQuestRewards(req.getVersion(), req.getValue());

        if (change == null) {
            return session.encodeMsg(NetMsgId.quest_dorm_reward_receive_failed_ack);
        }

        // Build response
        var rsp = QuestDormRewardReceiveSucceedAck.newInstance()
            .setChangeInfo(change.toProto())
            .setTaskGroup(
                UI32V.newInstance()
                    .setValue(dorm.getQuestGroupState())
                    .setVersion(dorm.getQuestGroup())
            );

        // Encode and send
        return session.encodeMsg(NetMsgId.quest_dorm_reward_receive_succeed_ack, rsp);
    }

}
