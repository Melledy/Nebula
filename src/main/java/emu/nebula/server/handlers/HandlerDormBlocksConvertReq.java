package emu.nebula.server.handlers;

import emu.nebula.net.GameSession;
import emu.nebula.net.HandlerId;
import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.Public.UI32;

@HandlerId(NetMsgId.dorm_blocks_convert_req)
public class HandlerDormBlocksConvertReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        // Parse request
        var req = UI32.parseFrom(message);

        // Convert blocks
        var change = session.getPlayer().getDormManager().convertBlocks(req.getValue());
        if (change == null) {
            return session.encodeMsg(NetMsgId.dorm_blocks_convert_failed_ack);
        }

        // Encode and send
        return session.encodeMsg(NetMsgId.dorm_blocks_convert_succeed_ack, change.toProto());
    }

}
