package emu.nebula.server.handlers;

import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.net.HandlerId;
import emu.nebula.net.GameSession;

@HandlerId(NetMsgId.energy_info_req)
public class HandlerEnergyInfoReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        // Encode and send player energy info
        return session.encodeMsg(NetMsgId.energy_info_succeed_ack, session.getPlayer().getEnergyProto());
    }

}
