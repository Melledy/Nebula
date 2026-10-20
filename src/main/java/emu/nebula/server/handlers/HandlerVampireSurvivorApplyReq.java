package emu.nebula.server.handlers;

import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.VampireSurvivorApply.VampireSurvivorApplyReq;
import emu.nebula.proto.VampireSurvivorApply.VampireSurvivorApplyResp;
import emu.nebula.net.HandlerId;
import emu.nebula.net.GameSession;

@HandlerId(NetMsgId.vampire_survivor_apply_req)
public class HandlerVampireSurvivorApplyReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        // Parse request
        var req = VampireSurvivorApplyReq.parseFrom(message);
        
        // Apply
        var game = session.getPlayer().getVampireSurvivorManager().apply(req.getId(), req.getBuilds());
        
        if (game == null) {
            return session.encodeMsg(NetMsgId.vampire_survivor_apply_failed_ack);
        }
        
        // Build response
        var rsp = VampireSurvivorApplyResp.newInstance()
                .setReward(game.getRewardProto());
        
        rsp.getMutableSelect();
        
        // Encode and send
        return session.encodeMsg(NetMsgId.vampire_survivor_apply_succeed_ack, rsp);
    }

}
