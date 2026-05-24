package emu.nebula.server.handlers;

import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.MallMonthlycardList.MallMonthlyCardList;
import emu.nebula.net.HandlerId;
import emu.nebula.data.GameData;
import emu.nebula.net.GameSession;

@HandlerId(NetMsgId.mall_monthlyCard_list_req)
public class HandlerMallMonthlyCardListReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        var player = session.getPlayer();
        var rsp = MallMonthlyCardList.newInstance();

        for (var data : GameData.getMallMonthlyCardDataTable()) {
            rsp.addList(data.toInfo(player));
        }

        return session.encodeMsg(NetMsgId.mall_monthlyCard_list_succeed_ack, rsp);
    }

}
