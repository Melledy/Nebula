package emu.nebula.server.handlers;

import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.Public.UI32;
import emu.nebula.proto.TowerGrowthGroupNodeUnlock.TowerGrowthGroupNodeUnlockResp;
import it.unimi.dsi.fastutil.ints.IntList;
import emu.nebula.net.HandlerId;
import emu.nebula.net.GameSession;

@HandlerId(NetMsgId.tower_growth_group_node_unlock_req)
public class HandlerTowerGrowthGroupNodeUnlockReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        // Parse request
        var req = UI32.parseFrom(message);
        
        // Quick unlock
        var change = session.getPlayer().getStarTowerManager().unlockGrowthNodeGroup(req.getValue());
        
        if (change == null) {
            session.encodeMsg(NetMsgId.tower_growth_group_node_unlock_failed_ack);
        }
        
        // Get list of unlocked nodes
        var unlocked = (IntList) change.getExtraData();
        
        // Build response
        var rsp = TowerGrowthGroupNodeUnlockResp.newInstance()
                .setChangeInfo(change.toProto());
        
        for (int nodeId : unlocked) {
            rsp.addNodes(nodeId);
        }
        
        // Encode and send
        return session.encodeMsg(NetMsgId.tower_growth_group_node_unlock_succeed_ack, rsp);
    }

}
