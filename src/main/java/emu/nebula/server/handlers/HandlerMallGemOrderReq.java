package emu.nebula.server.handlers;

import emu.nebula.Nebula;
import emu.nebula.data.GameData;
import emu.nebula.net.GameSession;
import emu.nebula.net.HandlerId;
import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.MallGemOrder.OrderInfo;

@HandlerId(NetMsgId.mall_gem_order_req)
public class HandlerMallGemOrderReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        var req = OrderInfo.parseFrom(message);
        var data = GameData.getMallGemDataTable().get(req.getId().hashCode());
        if (data == null) {
            return session.encodeMsg(NetMsgId.mall_gem_order_failed_ack);
        }

        var order = Nebula.getGameContext().getPayModule().createGemOrder(session, data);
        if (order == null) {
            return session.encodeMsg(NetMsgId.mall_gem_order_failed_ack);
        }

        return session.encodeMsg(NetMsgId.mall_gem_order_succeed_ack, order);
    }

}
