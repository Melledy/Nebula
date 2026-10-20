package emu.nebula.server.handlers;

import emu.nebula.net.GameSession;
import emu.nebula.net.HandlerId;
import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.Public.UI32;

@HandlerId(NetMsgId.dorm_furniture_preset_delete_req)
public class HandlerDormFurniturePresetDeleteReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        // The client does not send a request body for this packet in the current client version.
        // If a body is present, treat field 1 as the preset/build id.
        int buildId = 0;

        if (message != null && message.length > 0) {
            try {
                buildId = UI32.parseFrom(message).getValue();
            } catch (Exception ignored) {
                buildId = 0;
            }
        }

        if (buildId > 0) {
            session.getPlayer().getDormManager().deletePreset(buildId);
        }

        // Encode and send
        return session.encodeMsg(NetMsgId.dorm_furniture_preset_delete_succeed_ack);
    }

}
