package emu.nebula.server.handlers;

import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.CharGemInstanceSweep.CharGemInstanceSweepReq;
import emu.nebula.proto.CharGemInstanceSweep.CharGemInstanceSweepResp;
import emu.nebula.proto.CharGemInstanceSweep.CharGemInstanceSweepReward;
import emu.nebula.net.HandlerId;

import java.util.List;

import emu.nebula.data.GameData;
import emu.nebula.game.inventory.ItemParamMap;
import emu.nebula.game.quest.QuestCondition;
import emu.nebula.net.GameSession;

@HandlerId(NetMsgId.char_gem_instance_sweep_req)
public class HandlerCharGemInstanceSweepReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        // Parse request
        var req = CharGemInstanceSweepReq.parseFrom(message);
        
        // Get instance data
        var data = GameData.getCharGemInstanceDataTable().get(req.getId());
        if (data == null) {
            return session.encodeMsg(NetMsgId.char_gem_instance_sweep_failed_ack);
        }
        
        // Check reward group
        if (data.getDropId() > 0 && data.getRewardGroup(req.getRewardType()) == null) {
            return session.encodeMsg(NetMsgId.char_gem_instance_sweep_failed_ack);
        }
        
        // Sweep
        var change = session.getPlayer().getInstanceManager().sweepInstance(
                data,
                QuestCondition.CharGemInstanceClearTotal,
                session.getPlayer().getProgress().getCharGemLog(),
                req.getRewardType(),
                req.getTimes()
        );
        
        // Sanity check
        if (change == null) {
            return session.encodeMsg(NetMsgId.char_gem_instance_sweep_failed_ack);
        }
        
        // Build response
        var rsp = CharGemInstanceSweepResp.newInstance()
                .setChange(change.toProto());
        
        // Add reward list to response
        if (change.getExtraData() != null) {
            @SuppressWarnings("unchecked")
            var list = (List<ItemParamMap>) change.getExtraData();
            
            for (var rewards : list) {
                var reward = CharGemInstanceSweepReward.newInstance()
                        .setExp(data.getEnergyConsume());
                
                rewards.toItemTemplateStream().forEach(reward::addAwardItems);
                
                rsp.addRewards(reward);
            }
        }
        
        // Send response
        return session.encodeMsg(NetMsgId.char_gem_instance_sweep_succeed_ack, rsp);
    }

}
