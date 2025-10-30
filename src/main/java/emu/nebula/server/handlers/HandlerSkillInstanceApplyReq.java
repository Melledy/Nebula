package emu.nebula.server.handlers;

import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.SkillInstanceApply.SkillInstanceApplyReq;
import emu.nebula.net.HandlerId;
import emu.nebula.data.GameData;
import emu.nebula.net.GameSession;

@HandlerId(NetMsgId.skill_instance_apply_req)
public class HandlerSkillInstanceApplyReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        // Parse request
        var req = SkillInstanceApplyReq.parseFrom(message);
        
        var data = GameData.getSkillInstanceDataTable().get(req.getId());
        if (data == null || !data.hasEnergy(session.getPlayer())) {
            return this.encodeMsg(NetMsgId.skill_instance_apply_failed_ack);
        }
        
        // Set player instance id
        session.getPlayer().getInstanceManager().setCurInstanceId(req.getId());
        
        // Send response
        return this.encodeMsg(NetMsgId.skill_instance_apply_succeed_ack);
    }

}
