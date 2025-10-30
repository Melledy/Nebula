package emu.nebula.server.handlers;

import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.DailyInstanceApply.DailyInstanceApplyReq;
import emu.nebula.net.HandlerId;
import emu.nebula.data.GameData;
import emu.nebula.net.GameSession;

@HandlerId(NetMsgId.daily_instance_apply_req)
public class HandlerDailyInstanceApplyReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        // Parse request
        var req = DailyInstanceApplyReq.parseFrom(message);
        
        var data = GameData.getDailyInstanceDataTable().get(req.getId());
        if (data == null || !data.hasEnergy(session.getPlayer())) {
            return this.encodeMsg(NetMsgId.daily_instance_apply_failed_ack);
        }
        
        // Check reward group
        if (data.getRewardGroup(req.getRewardType()) == null) {
            return this.encodeMsg(NetMsgId.daily_instance_apply_failed_ack);
        }
        
        // Set player
        session.getPlayer().getInstanceManager().setCurInstanceId(req.getId(), req.getRewardType());
        
        // Send response
        return this.encodeMsg(NetMsgId.daily_instance_apply_succeed_ack);
    }

}
