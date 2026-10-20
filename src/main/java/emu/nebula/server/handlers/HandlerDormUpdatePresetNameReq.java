package emu.nebula.server.handlers;

import emu.nebula.net.GameSession;
import emu.nebula.net.HandlerId;
import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.Public.StringV;

@HandlerId(NetMsgId.dorm_update_preset_name_req)
public class HandlerDormUpdatePresetNameReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        // Parse request
        var req = StringV.parseFrom(message);

        // Version = preset/build id, Value = new name
        if (!session.getPlayer().getDormManager().renamePreset(req.getVersion(), req.getValue())) {
            return session.encodeMsg(NetMsgId.dorm_update_preset_name_failed_ack);
        }

        // Encode and send
        return session.encodeMsg(NetMsgId.dorm_update_preset_name_succeed_ack);
    }

}
