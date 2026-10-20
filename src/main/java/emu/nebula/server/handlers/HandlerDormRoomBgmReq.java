package emu.nebula.server.handlers;

import emu.nebula.net.GameSession;
import emu.nebula.net.HandlerId;
import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.DormRoomBgm.DormRoomBGMReq;

@HandlerId(NetMsgId.dorm_room_bgm_req)
public class HandlerDormRoomBgmReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        // Parse request
        var req = DormRoomBGMReq.parseFrom(message);

        var manager = session.getPlayer().getDormManager();

        // Validate the whole request before applying it
        for (var bgm : req.getBGMList()) {
            if (!manager.isRoomUnlocked(bgm.getDormRoomId()) || bgm.getDiscId() < 0) {
                return session.encodeMsg(NetMsgId.dorm_room_bgm_failed_ack);
            }
        }

        for (var bgm : req.getBGMList()) {
            manager.setRoomBgm(bgm.getDormRoomId(), bgm.getDiscId());
        }

        // Encode and send
        return session.encodeMsg(NetMsgId.dorm_room_bgm_succeed_ack);
    }

}
