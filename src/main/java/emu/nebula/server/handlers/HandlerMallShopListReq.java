package emu.nebula.server.handlers;

import emu.nebula.GameConstants;
import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.MallShopList;
import emu.nebula.proto.MallShopList.MallShopProductList;
import emu.nebula.net.HandlerId;
import emu.nebula.data.GameData;
import emu.nebula.net.GameSession;

@HandlerId(NetMsgId.mall_shop_list_req)
public class HandlerMallShopListReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        var rsp = MallShopProductList.newInstance();

        for (var data : GameData.getMallShopDataTable()) {
            if (!data.isVisible()) {
                continue;
            }

            var info = MallShopList.ProductInfo.newInstance()
                    .setId(data.getIdString())
                    .setStock(data.getStock() > 0 ? data.getStock(session.getPlayer()) : GameConstants.UNLIMITED_STOCK)
                    .setRefreshTime(data.getNextRefreshTime());

            rsp.addList(info);
        }

        return session.encodeMsg(NetMsgId.mall_shop_list_succeed_ack, rsp);
    }

}
