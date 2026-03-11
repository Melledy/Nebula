package emu.nebula.server.handlers;

import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.MallPackageListOuterClass.MallPackageList;
import emu.nebula.proto.MallPackageListOuterClass.PackageInfo;
import emu.nebula.net.HandlerId;
import emu.nebula.data.GameData;
import emu.nebula.net.GameSession;

@HandlerId(NetMsgId.mall_package_list_req)
public class HandlerMallPackageListReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
      
        var rsp = MallPackageList.newInstance();
        var inventory = session.getPlayer().getInventory();
        
        for (var data : GameData.getMallPackageDataTable()) {
            int buyCount = inventory.getMallBuyCount().getOrDefault(data.getIdString(), 0);
            int stock = data.getStock();
            if (stock > 0) {
                stock = stock - (buyCount % stock);
            } else {
                stock = 0;
            }
            
            var info = PackageInfo.newInstance()
                    .setId(data.getIdString())
                    .setStock(stock);
            
            rsp.addList(info);
        }
        
        return session.encodeMsg(NetMsgId.mall_package_list_succeed_ack, rsp);
    }
}
