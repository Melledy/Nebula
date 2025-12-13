package emu.nebula.server.handlers;

import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.InfinityTowerInfo.InfinityTowerInfoResp;
import emu.nebula.proto.Public.InfinityTowerLevelInfo;
import emu.nebula.net.HandlerId;
import emu.nebula.net.GameSession;

@HandlerId(NetMsgId.infinity_tower_info_req)
public class HandlerInfinityTowerInfoReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        // Build response
        var rsp = InfinityTowerInfoResp.newInstance()
                .setBountyLevel(session.getPlayer().getInfinityTowerManager().getBountyLevel());
        
        // Get infinite arena log from player progress
        for (var entry : session.getPlayer().getProgress().getInfinityTowerLog().int2IntEntrySet()) {
            var info = InfinityTowerLevelInfo.newInstance()
                    .setId(entry.getIntKey())
                    .setLevelId(entry.getIntValue());
            
            rsp.addInfos(info);
        }
        
        // Encode and send
        return session.encodeMsg(NetMsgId.infinity_tower_info_succeed_ack, rsp);
    }

}
