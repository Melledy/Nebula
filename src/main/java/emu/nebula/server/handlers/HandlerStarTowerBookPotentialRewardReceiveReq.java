package emu.nebula.server.handlers;

import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.Public.UI32;
import emu.nebula.proto.StarTowerBookPotentialRewardReceive.StarTowerBookPotentialRewardReceiveResp;
import it.unimi.dsi.fastutil.ints.IntList;
import emu.nebula.net.HandlerId;
import emu.nebula.net.GameSession;

@HandlerId(NetMsgId.star_tower_book_potential_reward_receive_req)
public class HandlerStarTowerBookPotentialRewardReceiveReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        // Parse character id
        var req = UI32.parseFrom(message);
        var character = session.getPlayer().getCharacters().getCharacterById(req.getValue());
        
        if (character == null) {
            return session.encodeMsg(NetMsgId.star_tower_book_potential_reward_receive_failed_ack);
        }
        
        // Claim rewards
        var change = character.claimPotentialBookRewards();
        
        if (change == null) {
            return session.encodeMsg(NetMsgId.star_tower_book_potential_reward_receive_failed_ack);
        }
        
        // Build response
        var rsp = StarTowerBookPotentialRewardReceiveResp.newInstance()
                .setChange(change.toProto());
        
        // Add list of received reward ids to response
        var received = (IntList) change.getExtraData();
        received.forEach(rsp::addReceivedIds);
        
        // Encode and send
        return session.encodeMsg(NetMsgId.star_tower_book_potential_reward_receive_succeed_ack, rsp);
    }

}
