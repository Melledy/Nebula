package emu.nebula.server.handlers;

import emu.nebula.Nebula;
import emu.nebula.data.GameData;
import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.net.HandlerId;
import emu.nebula.net.GameSession;
import emu.nebula.proto.MallGemOrder.OrderInfo;
import emu.nebula.proto.MallPackageOrderOuterClass.MallPackageOrder;

@HandlerId(NetMsgId.mall_package_order_req)
public class HandlerMallPackageOrderReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        var req = OrderInfo.parseFrom(message);
        var data = GameData.getMallPackageDataTable().get(req.getId().hashCode());
        if (data == null) {
            return session.encodeMsg(NetMsgId.mall_package_order_failed_ack);
        }

        if (data.isCashPackage()) {
            var order = Nebula.getGameContext().getPayModule().createPackageOrder(session, data);
            if (order == null) {
                return session.encodeMsg(NetMsgId.mall_package_order_failed_ack);
            }

            var rsp = MallPackageOrder.newInstance().setOrder(order);
            if (order.hasNextPackage()) {
                rsp.setNextPackage(order.getNextPackage().toArray());
            }

            return session.encodeMsg(
                    NetMsgId.mall_package_order_succeed_ack,
                    rsp
            );
        }

        var change = session.getPlayer().getInventory().buyMallPackage(data);
        if (change == null) {
            return session.encodeMsg(NetMsgId.mall_package_order_failed_ack);
        }

        if (data.isFreePackage()) {
            session.getPlayer().queueMallPackageStateNotify();
        }

        return session.encodeMsg(
                NetMsgId.mall_package_order_succeed_ack,
                MallPackageOrder.newInstance().setChange(change.toProto())
        );
    }

}
