package emu.nebula.server.handlers;

import emu.nebula.Nebula;
import emu.nebula.net.GameSession;
import emu.nebula.net.HandlerId;
import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;

@HandlerId(NetMsgId.mall_order_cancel_req)
public class HandlerMallOrderCancelReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        var payModule = Nebula.getGameContext().getPayModule();
        payModule.clearPendingCollect(session.getPlayer().getUid());
        return session.encodeMsg(NetMsgId.mall_order_cancel_succeed_ack);
    }

}
