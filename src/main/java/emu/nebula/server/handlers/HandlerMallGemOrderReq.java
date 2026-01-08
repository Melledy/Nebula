package emu.nebula.server.handlers;

import emu.nebula.Nebula;

import emu.nebula.data.GameData;
import emu.nebula.net.GameSession;
import emu.nebula.net.HandlerId;
import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.MallGemOrder.OrderInfo;
import emu.nebula.game.player.PlayerChangeInfo;

@HandlerId(NetMsgId.mall_gem_order_req)
public class HandlerMallGemOrderReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        // Parse req
        var req = OrderInfo.parseFrom(message);
        
        Nebula.getLogger().info("HandlerMallGemOrderReq called. ID: " + req.getId());
        
        // Get data
        var def = GameData.getMallGemDataTable().get(req.getId().hashCode());
        
        if (def == null) {
            Nebula.getLogger().info("MallGemDef not found for ID: " + req.getId() + " Hash: " + req.getId().hashCode());
            return session.encodeMsg(NetMsgId.mall_gem_order_failed_ack);
        }
        
        // Change info
        var change = new PlayerChangeInfo();
        
        // Add Base Item
        int baseId = def.getBaseItemId();
        int baseQty = def.getBaseItemQty();
        
        if (baseId > 0 && baseQty > 0) {
            session.getPlayer().getInventory().addItem(baseId, baseQty, change);
            Nebula.getLogger().info("Awarding Base Item: " + baseId + " Qty: " + baseQty);
        }
        
        // Add Bonus Item
        // Currently assuming Maiden (First Top-up) bonus as HandlerMallGemListReq sets Maiden=true
        int bonusId = def.getMaidenBonusItemId();
        int bonusQty = def.getMaidenBonusItemQty();
        
        if (bonusId > 0 && bonusQty > 0) {
             session.getPlayer().getInventory().addItem(bonusId, bonusQty, change);
             Nebula.getLogger().info("Awarding Bonus Item: " + bonusId + " Qty: " + bonusQty);
        }
        
        // Add next package
        session.getPlayer().addNextPackage(NetMsgId.items_change_notify, change.toProto());
        
        // Build success response
        // We echo back the ID
        var rsp = OrderInfo.newInstance()
                .setId(req.getId());
        
        // Send
        return session.encodeMsg(NetMsgId.mall_gem_order_succeed_ack, rsp);
    }
}
