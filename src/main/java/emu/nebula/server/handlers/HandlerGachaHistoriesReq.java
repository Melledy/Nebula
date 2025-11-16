package emu.nebula.server.handlers;

import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.GachaHistoriesOuterClass.GachaHistories;
import emu.nebula.proto.Public.UI32;
import emu.nebula.net.HandlerId;
import emu.nebula.net.GameSession;

@HandlerId(NetMsgId.gacha_histories_req)
public class HandlerGachaHistoriesReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        // Parse request
        var req = UI32.parseFrom(message);
        
        // Get history log
        var list = session.getPlayer().getGachaManager().getHistories().get(req.getValue());
        
        // Build response
        var rsp = GachaHistories.newInstance();
        
        if (list != null) {
            for (var log : list) {
                rsp.addList(log.toProto());
            }
        }
        
        // Encode and send
        return session.encodeMsg(NetMsgId.gacha_histories_succeed_ack, rsp);
    }

}
