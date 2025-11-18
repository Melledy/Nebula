package emu.nebula.server.handlers;

import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.TowerGrowthDetail.TowerGrowthDetailResp;
import emu.nebula.net.HandlerId;
import emu.nebula.net.GameSession;

@HandlerId(NetMsgId.tower_growth_detail_req)
public class HandlerTowerGrowthDetailReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        // Build response
        var rsp = TowerGrowthDetailResp.newInstance()
                .addAllDetail(session.getPlayer().getProgress().getStarTowerGrowth());
        
        // Encode and send
        return session.encodeMsg(NetMsgId.tower_growth_detail_succeed_ack, rsp);
    }

}
