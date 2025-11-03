package emu.nebula.server.handlers;

import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.PlayerHeadInfo.PlayerHeadIconInfoResp;
import emu.nebula.net.HandlerId;
import emu.nebula.net.GameSession;

@HandlerId(NetMsgId.player_head_icon_info_req)
public class HandlerPlayerHeadIconInfoReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        // Build response
        var rsp = PlayerHeadIconInfoResp.newInstance();
        
        var icons = session.getPlayer().getInventory().getAllHeadIcons();
        
        for (int id : icons) {
            rsp.addList(id);
        }
        
        // Encode and send
        return session.encodeMsg(NetMsgId.player_head_icon_info_succeed_ack, rsp);
    }

}
