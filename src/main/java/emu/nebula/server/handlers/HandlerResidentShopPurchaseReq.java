package emu.nebula.server.handlers;

import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.ResidentShopPurchase.ResidentShopPurchaseReq;
import emu.nebula.proto.ResidentShopPurchase.ResidentShopPurchaseResp;
import emu.nebula.net.HandlerId;
import emu.nebula.data.GameData;
import emu.nebula.net.GameSession;

@HandlerId(NetMsgId.resident_shop_purchase_req)
public class HandlerResidentShopPurchaseReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        // Parse request
        var req = ResidentShopPurchaseReq.parseFrom(message);
        
        // Get goods
        var data = GameData.getResidentGoodsDataTable().get(req.getGoodsId());
        if (data == null || !data.isVisible(session.getPlayer())) {
            return session.encodeMsg(NetMsgId.resident_shop_purchase_failed_ack);
        }
        
        // Buy
        var change = session.getPlayer().getInventory().buyShopItem(data, req.getNumber());
        
        if (change == null) {
            return session.encodeMsg(NetMsgId.resident_shop_purchase_failed_ack);
        }
        
        // Build response
        var rsp = ResidentShopPurchaseResp.newInstance()
                .setChange(change.toProto())
                .setPurchasedNumber(req.getNumber());

        var shopData = GameData.getResidentShopDataTable().get(data.getShopId());
        if (shopData == null || !shopData.isVisible()) {
            return session.encodeMsg(NetMsgId.resident_shop_purchase_failed_ack);
        }
        
        rsp.getMutableShop()
            .setId(data.getShopId())
            .setRefreshTime(shopData.getNextRefreshTime());
        
        // Encode and send
        return session.encodeMsg(NetMsgId.resident_shop_purchase_succeed_ack, rsp);
    }

}
