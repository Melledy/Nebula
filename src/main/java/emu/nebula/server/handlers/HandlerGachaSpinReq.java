package emu.nebula.server.handlers;

import emu.nebula.Nebula;
import emu.nebula.data.GameData;
import emu.nebula.game.gacha.GachaMode;
import emu.nebula.net.GameSession;
import emu.nebula.net.HandlerId;
import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.GachaSpin.GachaSpinReq;

@HandlerId(NetMsgId.gacha_spin_req)
public class HandlerGachaSpinReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        // Parse request
        var req = GachaSpinReq.parseFrom(message);
        Integer amount = GachaMode.getAmountByMode(req.getMode());
        if (amount == null) {
            return session.encodeMsg(NetMsgId.gacha_spin_failed_ack);
        }
        
        // Do gacha
        var result = Nebula.getGameContext().getGachaModule().spin(
            session.getPlayer(),
            req.getId(),
            amount
        );
        
        if (result == null) {
            return session.encodeMsg(NetMsgId.gacha_spin_failed_ack);
        }

        var gachaData = GameData.getGachaDataTable().get(req.getId());
        var rsp = result.toSpinResp(gachaData);
        
        // Encode and send response
        return session.encodeMsg(NetMsgId.gacha_spin_succeed_ack, rsp);
    }

}
