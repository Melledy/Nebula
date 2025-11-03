package emu.nebula.server.handlers;

import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.PlayerHeadiconSet.PlayerHeadIconSetReq;
import emu.nebula.net.HandlerId;
import emu.nebula.net.GameSession;

@HandlerId(NetMsgId.player_head_icon_set_req)
public class HandlerPlayerHeadIconSetReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        // Parse req
        var req = PlayerHeadIconSetReq.parseFrom(message);
        
        // Set head icon
        boolean result = session.getPlayer().editHeadIcon(req.getHeadIcon());
        
        // Encode and send
        return session.encodeMsg(result ? NetMsgId.player_head_icon_set_succeed_ack : NetMsgId.player_head_icon_set_failed_ack);
    }

}
