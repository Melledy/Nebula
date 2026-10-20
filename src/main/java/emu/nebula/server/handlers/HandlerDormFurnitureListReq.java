package emu.nebula.server.handlers;

import emu.nebula.net.GameSession;
import emu.nebula.net.HandlerId;
import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.DormFurnitureList.DormBagListResp;
import emu.nebula.proto.Public.ItemTpl;
import emu.nebula.proto.PublicDorm.DormBagInfo;

@HandlerId(NetMsgId.dorm_bag_list_req)
public class HandlerDormFurnitureListReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        // Get dorm manager
        var dorm = session.getPlayer().getDormManager();
        
        // Build response
        var rsp = DormBagListResp.newInstance();
        
        // Encode dorm furniture inventory
        for (var entry : dorm.getFurnitureCollection().int2IntEntrySet()) {
            if (entry.getIntValue() <= 0) {
                continue;
            }
            
            var item = ItemTpl.newInstance()
                .setTid(entry.getIntKey())
                .setQty(entry.getIntValue());
            
            var info = DormBagInfo.newInstance()
                    .setItemTplInfo(item);

            rsp.addDormBagList(info);
        }

        // Encode and send
        return session.encodeMsg(NetMsgId.dorm_bag_list_succeed_ack, rsp);
    }

}
