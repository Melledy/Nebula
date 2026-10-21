package emu.nebula.server.handlers;

import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.CharGemInstanceApply.CharGemInstanceApplyReq;
import emu.nebula.net.HandlerId;
import emu.nebula.data.GameData;
import emu.nebula.net.GameSession;

@HandlerId(NetMsgId.char_gem_instance_apply_req)
public class HandlerCharGemInstanceApplyReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        // Parse request
        var req = CharGemInstanceApplyReq.parseFrom(message);
        
        var data = GameData.getCharGemInstanceDataTable().get(req.getId());
        if (data == null || !data.hasEnergy(session.getPlayer())) {
            return session.encodeMsg(NetMsgId.char_gem_instance_apply_failed_ack);
        }
        
        // Check reward group
        if (data.getDropId() > 0 && data.getRewardGroup(req.getRewardType()) == null) {
            return session.encodeMsg(NetMsgId.char_gem_instance_apply_failed_ack);
        }
        
        // Set player instance id
        session.getPlayer().getInstanceManager().setCurInstanceId(req.getId(), req.getRewardType());
        
        // Send response
        return session.encodeMsg(NetMsgId.char_gem_instance_apply_succeed_ack);
    }

}
