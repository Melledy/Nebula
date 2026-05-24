package emu.nebula.server.handlers;

import emu.nebula.Nebula;
import emu.nebula.data.GameData;
import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.net.HandlerId;
import emu.nebula.net.GameSession;
import emu.nebula.proto.MallMonthlycardList.MonthlyCardInfo;

@HandlerId(NetMsgId.mall_monthlyCard_order_req)
public class HandlerMallMonthlyCardOrderReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        var req = MonthlyCardInfo.parseFrom(message);
        var data = GameData.getMallMonthlyCardDataTable().get(req.getId().hashCode());
        var order = Nebula.getGameContext().getPayModule().createMonthlyCardOrder(session, data);
        if (order == null) {
            return session.encodeMsg(NetMsgId.mall_monthlyCard_order_failed_ack);
        }

        return session.encodeMsg(NetMsgId.mall_monthlyCard_order_succeed_ack, order);
    }

}
