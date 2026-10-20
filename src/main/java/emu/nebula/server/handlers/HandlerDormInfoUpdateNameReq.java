package emu.nebula.server.handlers;

import emu.nebula.net.GameSession;
import emu.nebula.net.HandlerId;
import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.Public.StringV;

@HandlerId(NetMsgId.dorm_info_update_name_req)
public class HandlerDormInfoUpdateNameReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        // Parse request
        var req = StringV.parseFrom(message);

        // Version = 0 means the dorm name, otherwise the room id
        if (!session.getPlayer().getDormManager().renameRoom(req.getVersion(), req.getValue())) {
            return session.encodeMsg(NetMsgId.dorm_info_update_name_failed_ack);
        }

        // Encode and send
        return session.encodeMsg(NetMsgId.dorm_info_update_name_succeed_ack);
    }

}
