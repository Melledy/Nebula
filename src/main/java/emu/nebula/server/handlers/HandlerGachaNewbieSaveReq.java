package emu.nebula.server.handlers;

import emu.nebula.Nebula;
import emu.nebula.net.GameSession;
import emu.nebula.net.HandlerId;
import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.GachaNewbieSave.GachaNewbieSaveReq;

@HandlerId(NetMsgId.gacha_newbie_save_req)
public class HandlerGachaNewbieSaveReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        var req = GachaNewbieSaveReq.parseFrom(message);
        Integer index = req.hasIdx() ? req.getIdx() : null;
        boolean succeeded = Nebula.getGameContext().getGachaModule().saveNewbie(session.getPlayer(), req.getId(), index);
        if (!succeeded) {
            return session.encodeMsg(NetMsgId.gacha_newbie_save_failed_ack);
        }

        return session.encodeMsg(NetMsgId.gacha_newbie_save_succeed_ack);
    }

}
