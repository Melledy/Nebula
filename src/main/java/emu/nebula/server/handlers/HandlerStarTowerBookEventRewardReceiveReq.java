package emu.nebula.server.handlers;

import emu.nebula.net.GameSession;
import emu.nebula.net.HandlerId;
import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.Public.UI32;
import emu.nebula.proto.StarTowerBookEventRewardReceive.StarTowerBookEventRewardReceiveResp;
import it.unimi.dsi.fastutil.ints.IntList;

@HandlerId(NetMsgId.star_tower_book_event_reward_receive_req)
public class HandlerStarTowerBookEventRewardReceiveReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        // Parse request
        var req = UI32.parseFrom(message);
        
        // Claim reward(s)
        var change = session.getPlayer().getQuestManager().receiveStarTowerBookEventReward(req.getValue());
        
        if (change == null) {
            return session.encodeMsg(NetMsgId.star_tower_book_event_reward_receive_failed_ack);
        }
        
        // Build response
        var rsp = StarTowerBookEventRewardReceiveResp.newInstance()
                .setChange(change.toProto());
        
        // Add list of received reward ids to response
        var received = (IntList) change.getExtraData();
        received.forEach(rsp::addReceivedIds);
        
        // Encode and send
        return session.encodeMsg(NetMsgId.star_tower_book_event_reward_receive_succeed_ack, rsp);
    }

}
