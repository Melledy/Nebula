package emu.nebula.server.handlers;

import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.Public.UI32;
import emu.nebula.proto.PublicStarTower.StarTowerBookPotential;
import emu.nebula.proto.StarTowerBookCharPotentialGet.StarTowerBookPotentialGetResp;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import emu.nebula.net.HandlerId;
import emu.nebula.data.GameData;
import emu.nebula.net.GameSession;

@HandlerId(NetMsgId.star_tower_book_char_potential_get_req)
public class HandlerStarTowerBookCharPotentialGetReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        // Parse character id
        var req = UI32.parseFrom(message);
        var character = session.getPlayer().getCharacters().getCharacterById(req.getValue());
        
        if (character == null) {
            return session.encodeMsg(NetMsgId.star_tower_book_char_potential_get_failed_ack);
        }
        
        // Get potentials
        var potentialDef = GameData.getCharPotentialDataTable().get(character.getCharId());
        
        if (potentialDef == null) {
            return session.encodeMsg(NetMsgId.star_tower_book_char_potential_get_failed_ack);
        }
        
        // Build response
        var rsp = StarTowerBookPotentialGetResp.newInstance();
        
        // Get count
        var potentials = new IntArrayList();
        
        potentials.addElements(potentials.size(), potentialDef.getMasterSpecificPotentialIds());
        potentials.addElements(potentials.size(), potentialDef.getAssistSpecificPotentialIds());
        potentials.addElements(potentials.size(), potentialDef.getCommonPotentialIds());
        potentials.addElements(potentials.size(), potentialDef.getMasterNormalPotentialIds());
        potentials.addElements(potentials.size(), potentialDef.getAssistNormalPotentialIds());
        
        // Serialize potentials
        for (var potentialId : potentials) {
            int level = character.getSeenPotentialLevel(potentialId);
            
            if (level > 0) {
                var info = StarTowerBookPotential.newInstance()
                        .setId(potentialId)
                        .setLevel(level);
                
                rsp.addPotentials(info);
            }
        }
        
        // Add received reward ids
        for (int rewardId : character.getReceivedPotentialRewards()) {
            rsp.addReceivedIds(rewardId);
        }
        
        // Encode and send
        return session.encodeMsg(NetMsgId.star_tower_book_char_potential_get_succeed_ack, rsp);
    }

}
