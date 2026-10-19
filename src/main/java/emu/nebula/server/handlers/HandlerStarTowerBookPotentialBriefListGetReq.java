package emu.nebula.server.handlers;

import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.PublicStarTower.StarTowerBookCharPotentialBrief;
import emu.nebula.proto.StarTowerBookPotentialBriefListGet.StarTowerBookPotentialBriefListResp;
import emu.nebula.net.HandlerId;
import emu.nebula.net.GameSession;

@HandlerId(NetMsgId.star_tower_book_potential_brief_list_get_req)
public class HandlerStarTowerBookPotentialBriefListGetReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        // Build response
        var rsp = StarTowerBookPotentialBriefListResp.newInstance();
        
        // Get all characters
        var characters = session.getPlayer().getCharacters().getCharacterCollection();
        
        for (var character : characters) {
            var info = StarTowerBookCharPotentialBrief.newInstance()
                    .setCharId(character.getCharId())
                    .setCount(character.getTotalSeenPotentials());
            
            rsp.addInfos(info);
        }
        
        // Encode and send
        return session.encodeMsg(NetMsgId.star_tower_book_potential_brief_list_get_succeed_ack, rsp);
    }

}
