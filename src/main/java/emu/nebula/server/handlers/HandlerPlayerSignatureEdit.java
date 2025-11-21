package emu.nebula.server.handlers;

import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.PlayerSignatureEdit.PlayerSignatureEditReq;
import emu.nebula.proto.Public.Error;
import emu.nebula.net.HandlerId;
import emu.nebula.Nebula;
import emu.nebula.net.GameSession;

@HandlerId(NetMsgId.player_signature_edit_req)
public class HandlerPlayerSignatureEdit extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        // Parse request
        var req = PlayerSignatureEditReq.parseFrom(message);
        var signature = req.getSignature();
        
        if (signature == null || signature.isEmpty()) {
            return session.encodeMsg(NetMsgId.player_signature_edit_failed_ack);
        }
        
        // Check if we need to handle a command
        if (signature.charAt(0) == '!' || signature.charAt(0) == '/') {
            Nebula.getCommandManager().invoke(session.getPlayer(), signature);
            return session.encodeMsg(
                    NetMsgId.player_signature_edit_failed_ack,
                    Error.newInstance().setCode(119902).addArguments("\nCommand Success")
            );
        }
        
        // Edit signature
        session.getPlayer().editSignature(req.getSignature());
        
        // Send response
        return session.encodeMsg(NetMsgId.player_signature_edit_succeed_ack);
    }

}
