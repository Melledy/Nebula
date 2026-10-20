package emu.nebula.server.handlers;

import emu.nebula.net.GameSession;
import emu.nebula.net.HandlerId;
import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.Public.UI32V;

@HandlerId(NetMsgId.dorm_set_room_scene_req)
public class HandlerDormSetRoomSceneReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        // Parse request
        var req = UI32V.parseFrom(message);

        // Version = room id, Value = scene item id (0 clears the scene)
        if (!session.getPlayer().getDormManager().setRoomScene(req.getVersion(), req.getValue())) {
            return session.encodeMsg(NetMsgId.dorm_set_room_scene_failed_ack);
        }

        // Encode and send
        return session.encodeMsg(NetMsgId.dorm_set_room_scene_succeed_ack);
    }

}
