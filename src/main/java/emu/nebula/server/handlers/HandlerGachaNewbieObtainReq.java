package emu.nebula.server.handlers;

import emu.nebula.Nebula;
import emu.nebula.net.GameSession;
import emu.nebula.net.HandlerId;
import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.GachaNewbieObtain.GachaNewbieObtainReq;

@HandlerId(NetMsgId.gacha_newbie_obtain_req)
public class HandlerGachaNewbieObtainReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        var req = GachaNewbieObtainReq.parseFrom(message);
        var change = Nebula.getGameContext().getGachaModule().obtainNewbie(session.getPlayer(), req.getId(), req.getIdx());
        if (change == null) {
            return session.encodeMsg(NetMsgId.gacha_newbie_obtain_failed_ack);
        }

        if (!change.isEmpty()) {
            session.getPlayer().addNextPackage(NetMsgId.items_change_notify, change.toProto());
        }

        return session.encodeMsg(NetMsgId.gacha_newbie_obtain_succeed_ack, change.toProto());
    }

}
