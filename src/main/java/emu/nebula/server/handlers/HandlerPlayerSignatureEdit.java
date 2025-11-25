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
            String commandLabel = signature.toLowerCase().trim();
            if (commandLabel.startsWith("!") || commandLabel.startsWith("/")) {
                commandLabel = commandLabel.substring(1).split(" ")[0];
            }
            
            Nebula.getCommandManager().invoke(session.getPlayer(), signature);
            
            // If this is the remote command, return the message
            if ("remote".equals(commandLabel)) {
                String remoteMessage = emu.nebula.command.commands.RemoteKeyCommand.getLastMessage();
                if (remoteMessage != null) {
                    return session.encodeMsg(
                            NetMsgId.player_signature_edit_failed_ack,
                            Error.newInstance().setCode(119902).addArguments("\n" + remoteMessage)
                    );
                }
            }
            
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
