package emu.nebula.server.handlers;

import emu.nebula.net.GameSession;
import emu.nebula.net.HandlerId;
import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.PublicSoldier.SoldierSeasonInfo;

@HandlerId(NetMsgId.soldier_season_info_req)
public class HandlerSoldierSeasonInfoReq extends NetHandler {
    @Override
    public byte[] handle(GameSession session, byte[] message) {
        // Template response to get rid of errors
        var rsp = SoldierSeasonInfo.newInstance()
                .setSeasonId(1);
        
        // Encode and send
        return session.encodeMsg(NetMsgId.soldier_season_info_succeed_ack, rsp);
    }
}
