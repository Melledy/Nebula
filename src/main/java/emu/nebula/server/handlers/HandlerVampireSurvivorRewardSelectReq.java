package emu.nebula.server.handlers;

import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.VampireSurvivorRewardSelect.VampireSurvivorRewardSelectReq;
import emu.nebula.proto.VampireSurvivorRewardSelect.VampireSurvivorRewardSelectResp;
import emu.nebula.net.HandlerId;
import emu.nebula.net.GameSession;

@HandlerId(NetMsgId.vampire_survivor_reward_select_req)
public class HandlerVampireSurvivorRewardSelectReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        // Parse
        var req = VampireSurvivorRewardSelectReq.parseFrom(message);

        // Get game
        var game = session.getPlayer().getVampireSurvivorManager().getGame();

        if (game == null) {
            return session.encodeMsg(NetMsgId.vampire_survivor_reward_select_failed_ack);
        }

        // Select
        int cardId = game.selectReward(req.getIndex(), req.getReRoll());

        if (cardId <= 0) {
            return session.encodeMsg(NetMsgId.vampire_survivor_reward_select_failed_ack);
        }

        // Build response
        var rsp = VampireSurvivorRewardSelectResp.newInstance();

        rsp.getMutableResp()
                .setFateCardId(cardId)
                .setReward(game.getRewardProto());

        // Encode and send
        return session.encodeMsg(NetMsgId.vampire_survivor_reward_select_succeed_ack, rsp);
    }

}
