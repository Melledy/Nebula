package emu.nebula.server.handlers;

import emu.nebula.game.player.PlayerChangeInfo;
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
        // Parse character id, 0 means claim for all characters
        var req = UI32.parseFrom(message);
        int charId = req.getValue();
        
        PlayerChangeInfo change;
        
        if (charId > 0) {
            // Get character
            var character = session.getPlayer().getCharacters().getCharacterById(charId);
            
            if (character == null) {
                return session.encodeMsg(NetMsgId.star_tower_book_potential_reward_receive_failed_ack);
            }
            
            // Claim rewards
            change = character.claimPotentialBookRewards();
        } else {
            // Claim rewards for all characters
            change = session.getPlayer().getCharacters().claimAllPotentialBookRewards();
        }
        
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
