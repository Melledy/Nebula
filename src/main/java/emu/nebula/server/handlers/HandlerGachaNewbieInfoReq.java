package emu.nebula.server.handlers;

import emu.nebula.Nebula;
import emu.nebula.net.GameSession;
import emu.nebula.net.HandlerId;
import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.GachaNewbieInfoOuterClass.GachaNewbieInfoResp;

@HandlerId(NetMsgId.gacha_newbie_info_req)
public class HandlerGachaNewbieInfoReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        var rsp = GachaNewbieInfoResp.newInstance();
        for (var info : Nebula.getGameContext().getGachaModule().listNewbieInfos(session.getPlayer())) {
            rsp.addList(info);
        }
        return session.encodeMsg(NetMsgId.gacha_newbie_info_succeed_ack, rsp);
    }

}
