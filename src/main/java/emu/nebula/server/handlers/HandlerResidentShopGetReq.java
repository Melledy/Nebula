package emu.nebula.server.handlers;

import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.Public.BoughtGoods;
import emu.nebula.proto.Public.ResidentShop;
import emu.nebula.proto.ResidentShopGet.ResidentShopGetReq;
import emu.nebula.proto.ResidentShopGet.ResidentShopGetResp;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import emu.nebula.net.HandlerId;
import emu.nebula.data.GameData;
import emu.nebula.net.GameSession;

@HandlerId(NetMsgId.resident_shop_get_req)
public class HandlerResidentShopGetReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        var req = ResidentShopGetReq.parseFrom(message);
        var requestedShopIds = req.getShopIds();
        IntSet requestedShopIdSet = null;

        if (requestedShopIds.length() > 0) {
            requestedShopIdSet = new IntOpenHashSet(requestedShopIds.length());
            for (int shopId : requestedShopIds) {
                requestedShopIdSet.add(shopId);
            }
        }

        // Get shops
        var shops = new Int2ObjectOpenHashMap<ResidentShop>();
        
        for (var data : GameData.getResidentShopDataTable()) {
            if (requestedShopIdSet != null && !requestedShopIdSet.contains(data.getId()) || !data.isVisible()) {
                continue;
            }

            var proto = ResidentShop.newInstance()
                    .setId(data.getId())
                    .setRefreshTime(data.getNextRefreshTime());
            
            shops.put(data.getId(), proto);
        }
        
        // Add bought goods
        for (var data : GameData.getResidentGoodsDataTable()) {
            if (!data.isVisible(session.getPlayer())) {
                continue;
            }

            int bought = session.getPlayer().getInventory().getShopBuyCount().get(data.getId());
            if (bought == 0) {
                continue;
            }
            
            var shop = shops.get(data.getShopId());
            if (shop == null) {
                continue;
            }
            
            var info = BoughtGoods.newInstance()
                    .setId(data.getId())
                    .setNumber(bought);
            
            shop.addInfos(info);
        }
        
        // Build response
        var rsp = ResidentShopGetResp.newInstance();
        
        for (var shop : shops.values()) {
            rsp.addShops(shop);
        }
        
        // Encode and send
        return session.encodeMsg(NetMsgId.resident_shop_get_succeed_ack, rsp);
    }

}
