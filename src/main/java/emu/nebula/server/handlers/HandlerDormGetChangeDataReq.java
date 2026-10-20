package emu.nebula.server.handlers;

import emu.nebula.net.GameSession;
import emu.nebula.net.HandlerId;
import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.DormGetChangeData.DormChangeDataResp;
import emu.nebula.proto.PublicDorm.DormChangeDataInfo;

@HandlerId(NetMsgId.dorm_get_change_data_req)
public class HandlerDormGetChangeDataReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        // Get player dorm
        var dorm = session.getPlayer().getDormManager();
        
        // Build response
        var info = DormChangeDataInfo.newInstance()
                .setDormName(dorm.getDormName())
                .setLikeNum(dorm.getLikeNum())
                .setDayVisitNum(dorm.getDayVisitNum())
                .setVisitTotalNum(dorm.getVisitTotalNum());
        
        var rsp = DormChangeDataResp.newInstance()
                .setDormChangeData(info);

        // Encode and send
        return session.encodeMsg(NetMsgId.dorm_get_change_data_succeed_ack, rsp);
    }

}
