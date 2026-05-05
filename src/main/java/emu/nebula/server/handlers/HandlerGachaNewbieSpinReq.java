package emu.nebula.server.handlers;

import emu.nebula.Nebula;
import emu.nebula.game.gacha.GachaMode;
import emu.nebula.net.GameSession;
import emu.nebula.net.HandlerId;
import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.GachaNewbieSpin.GachaNewbieSpinResp;
import emu.nebula.proto.GachaSpin.GachaSpinReq;

@HandlerId(NetMsgId.gacha_newbie_spin_req)
public class HandlerGachaNewbieSpinReq extends NetHandler {
    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        var req = GachaSpinReq.parseFrom(message);
        Integer amount = GachaMode.getAmountByMode(req.getMode());
        if (amount == null) {
            return session.encodeMsg(NetMsgId.gacha_newbie_spin_failed_ack);
        }

        int[] cards = Nebula.getGameContext().getGachaModule().spinNewbie(session.getPlayer(), req.getId());
        if (cards == null) {
            return session.encodeMsg(NetMsgId.gacha_newbie_spin_failed_ack);
        }

        var rsp = GachaNewbieSpinResp.newInstance();
        rsp.addAllCards(cards);

        return session.encodeMsg(NetMsgId.gacha_newbie_spin_succeed_ack, rsp);
    }

}
