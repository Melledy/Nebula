package emu.nebula.server.handlers;

import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.PlayerSkinShow.PlayerSkinShowReq;
import emu.nebula.net.HandlerId;
import emu.nebula.net.GameSession;

@HandlerId(NetMsgId.player_skin_show_req)
public class HandlerPlayerSkinShowReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        // Parse req
        var req = PlayerSkinShowReq.parseFrom(message);
        
        // Set player skin
        boolean success = session.getPlayer().setSkin(req.getSkinId());
        
        if (success == false) {
            session.encodeMsg(NetMsgId.player_skin_show_failed_ack);
        }
        
        // Encode and send
        return session.encodeMsg(NetMsgId.player_skin_show_succeed_ack);
    }

}
