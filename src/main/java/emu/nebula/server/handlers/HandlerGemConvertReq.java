package emu.nebula.server.handlers;

import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.Public.UI32;
import emu.nebula.net.HandlerId;
import emu.nebula.net.GameSession;

@HandlerId(NetMsgId.gem_convert_req)
public class HandlerGemConvertReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        // Parse request
        var req = UI32.parseFrom(message);
        
        // Convert gems
        var change = session.getPlayer().getInventory().convertGems(req.getValue());
        
        if (change == null) {
            return session.encodeMsg(NetMsgId.gem_convert_failed_ack);
        }
        
        // Encode and send
        return session.encodeMsg(NetMsgId.gem_convert_succeed_ack, change.toProto());
    }

}
