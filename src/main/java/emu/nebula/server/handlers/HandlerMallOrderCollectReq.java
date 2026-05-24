package emu.nebula.server.handlers;

import emu.nebula.Nebula;
import emu.nebula.net.GameSession;
import emu.nebula.net.HandlerId;
import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.Public.CollectResp;

@HandlerId(NetMsgId.mall_order_collect_req)
public class HandlerMallOrderCollectReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        var rsp = CollectResp.newInstance().setStatusValue(1);
        var payModule = Nebula.getGameContext().getPayModule();
        var collectResult = payModule.consumePendingCollect(session);

        if (collectResult != null && collectResult.hasDisplayChange()) {
            rsp.setItems(collectResult.getDisplayChange());
        }

        if (collectResult != null && collectResult.hasStateChange()) {
            payModule.pushInventoryNotifies(session, collectResult.getStateChange());
        }

        return session.encodeMsg(NetMsgId.mall_order_collect_succeed_ack, rsp);
    }

}
