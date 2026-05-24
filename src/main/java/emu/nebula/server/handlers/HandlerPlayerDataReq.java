package emu.nebula.server.handlers;

import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.net.HandlerId;
import emu.nebula.net.GameSession;

@HandlerId(NetMsgId.player_data_req)
public class HandlerPlayerDataReq extends NetHandler {
    
    public boolean requirePlayer() {
        return false;
    }

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        // Check if player has been created yet
        if (session.getPlayer() == null) {
            return session.encodeMsg(NetMsgId.player_new_notify);
        }

        session.getPlayer().queueMallPackageStateNotify();
        
        // Encode player data
        return session.encodeMsg(NetMsgId.player_data_succeed_ack, session.getPlayer().toProto());
    }

}
