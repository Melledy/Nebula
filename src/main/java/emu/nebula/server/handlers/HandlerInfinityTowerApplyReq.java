package emu.nebula.server.handlers;

import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.InfinityTowerApply.InfinityTowerApplyReq;
import emu.nebula.net.HandlerId;
import emu.nebula.net.GameSession;

@HandlerId(NetMsgId.infinity_tower_apply_req)
public class HandlerInfinityTowerApplyReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        // Template hanlder
        var req = InfinityTowerApplyReq.parseFrom(message);
        
        // Apply
        boolean success = session.getPlayer().getInfinityTowerManager().apply(req.getLevelId(), req.getBuild().getBuildId());
        
        if (!success) {
            return session.encodeMsg(NetMsgId.infinity_tower_apply_failed_ack);
        }
        
        // Encode and send
        return session.encodeMsg(NetMsgId.infinity_tower_apply_succeed_ack);
    }

}
