package emu.nebula.server.handlers;

import java.util.ArrayList;

import emu.nebula.game.dorm.DormFurniture;
import emu.nebula.net.GameSession;
import emu.nebula.net.HandlerId;
import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.DormFurnitureSave.DormFurnitureSaveReq;
import emu.nebula.proto.Public.I64;

@HandlerId(NetMsgId.dorm_furniture_save_req)
public class HandlerDormFurnitureSaveReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        // Parse request
        var req = DormFurnitureSaveReq.parseFrom(message);

        // Build furniture list
        var furnitureList = new ArrayList<DormFurniture>();

        for (var info : req.getFurniturePosInfoList()) {
            furnitureList.add(new DormFurniture(info));
        }

        // Save room or preset
        var createTime = session.getPlayer().getDormManager().saveFurnitureLayout(
            req.getDormRoomId(),
            furnitureList,
            req.getBuildId()
        );

        if (createTime == null) {
            return session.encodeMsg(NetMsgId.dorm_furniture_save_failed_ack);
        }

        // The client reads the preset creation time from Value
        var rsp = I64.newInstance()
                .setValue(createTime);

        // Encode and send
        return session.encodeMsg(NetMsgId.dorm_furniture_save_succeed_ack, rsp);
    }

}
