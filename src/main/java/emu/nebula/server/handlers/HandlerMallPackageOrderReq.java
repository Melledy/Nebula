package emu.nebula.server.handlers;

import emu.nebula.Nebula;
import emu.nebula.data.GameData;
import emu.nebula.game.player.PlayerChangeInfo;
import emu.nebula.net.GameSession;
import emu.nebula.net.HandlerId;
import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.MallGemOrder.OrderInfo;
import emu.nebula.proto.MallPackageOrderOuterClass.MallPackageOrder;

@HandlerId(NetMsgId.mall_package_order_req)
public class HandlerMallPackageOrderReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        try {
            // Parse request
            // Using OrderInfo as no specific Req proto exists
            var req = OrderInfo.parseFrom(message);
            
           
            
            // Get package definition
            var def = GameData.getMallPackageDataTable().get(req.getId().hashCode());
            
            if (def == null) {
              
                return session.encodeMsg(NetMsgId.mall_package_order_failed_ack);
            }
            
            // Check purchase type (Real Money vs In-Game Currency)
            // If CurrencyItemId > 0, it is an in-game purchase (e.g. Lumina)
            int currencyId = def.getCurrencyItemId();
            
           
            // Award Items (Initialize early to capture cost changes too)
           
            var rewardChange = new PlayerChangeInfo();
            
            if (currencyId > 0) {
                // Disabled per user request due to client UI bug (Empty Shop)
                return session.encodeMsg(NetMsgId.mall_package_order_failed_ack);
            } else {
                
            }
            
           
            // Update Buy Count
            session.getPlayer().getInventory().getMallBuyCount().merge(req.getId(), 1, Integer::sum);
            Nebula.getGameDatabase().update(session.getPlayer().getInventory(), session.getPlayer().getUid(), "mallBuyCount", session.getPlayer().getInventory().getMallBuyCount());
            
            // Add Products/Rewards to the SAME rewardChange
            if (def.getProducts() != null) {
                for (var entry : def.getProducts().int2IntEntrySet()) {
                    int itemId = entry.getIntKey();
                    int count = entry.getIntValue();
                    
                    if (itemId > 0 && count > 0) {
                        session.getPlayer().getInventory().addItem(itemId, count, rewardChange);
                    }
                }
            }
            
            // Prepare Response
            // Use MallPackageOrder to combine OrderInfo and ChangeInfo
            var pkgOrder = MallPackageOrder.newInstance()
                    .setOrder(req)
                    .setChange(rewardChange.toProto());
            
          
            
            // Send success ack
            return session.encodeMsg(NetMsgId.mall_package_order_succeed_ack, pkgOrder);
            
        } catch (Exception e) {
          
            return session.encodeMsg(NetMsgId.mall_package_order_failed_ack);
        }
    }
}
