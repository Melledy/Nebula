package emu.nebula.server.handlers;

import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.MallPackageListOuterClass;
import emu.nebula.proto.MallPackageListOuterClass.MallPackageList;
import emu.nebula.net.HandlerId;
import emu.nebula.data.GameData;
import emu.nebula.net.GameSession;

@HandlerId(NetMsgId.mall_package_list_req)
public class HandlerMallPackageListReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        var player = session.getPlayer();
        var rsp = MallPackageList.newInstance();
        player.queueMallPackageStateNotify();

        for (var data : GameData.getMallPackageDataTable()) {
            if (!data.isVisible(player)) {
                continue;
            }

            var info = MallPackageListOuterClass.PackageInfo.newInstance()
                    .setId(data.getIdString())
                    .setStock(data.getStock(player))
                    .setRefreshTime(data.getNextRefreshTime());

            rsp.addList(info);
        }

        return session.encodeMsg(NetMsgId.mall_package_list_succeed_ack, rsp);
    }

}
