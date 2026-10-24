package emu.nebula.server.handlers;

import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.NpcAffinityBookGet.NPCAffinityBookGetResp;
import emu.nebula.net.HandlerId;
import emu.nebula.net.GameSession;

@HandlerId(NetMsgId.npc_affinity_book_get_req)
public class HandlerNpcAffinityBookGetReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        var player = session.getPlayer();
        
        // Build response
        var rsp = NPCAffinityBookGetResp.newInstance();
        
        for (var npc : player.getStarTowerManager().getNpcAffinities()) {
            rsp.addInfos(npc.toProto());
        }
        
        // Encode and send
        return session.encodeMsg(NetMsgId.npc_affinity_book_get_succeed_ack, rsp);
    }

}
