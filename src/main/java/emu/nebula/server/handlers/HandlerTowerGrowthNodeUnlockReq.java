package emu.nebula.server.handlers;

import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.Public.UI32;
import emu.nebula.net.HandlerId;
import emu.nebula.net.GameSession;

@HandlerId(NetMsgId.tower_growth_node_unlock_req)
public class HandlerTowerGrowthNodeUnlockReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        // Parse request
        var req = UI32.parseFrom(message);
        
        // Unlock tower growth node
        var change = session.getPlayer().getStarTowerManager().unlockGrowthNode(req.getValue());
        
        if (change == null) {
            return session.encodeMsg(NetMsgId.tower_growth_node_unlock_failed_ack);
        }
        
        // Encode and send
        return session.encodeMsg(NetMsgId.tower_growth_node_unlock_succeed_ack, change.toProto());
    }

}
