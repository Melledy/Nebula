package emu.nebula.server.handlers;

import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.Public.I64;
import emu.nebula.net.HandlerId;
import emu.nebula.net.GameSession;

@HandlerId(NetMsgId.player_music_set_req)
public class HandlerPlayerMusicSetReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        // Parse request
        var req = I64.parseFrom(message);
        
        // Set music
        boolean success = session.getPlayer().setMusic(req.getValue());
        
        if (success == false) {
            return session.encodeMsg(NetMsgId.player_music_set_failed_ack);
        }
        
        // Encode and send
        return session.encodeMsg(NetMsgId.player_music_set_succeed_ack);
    }

}
