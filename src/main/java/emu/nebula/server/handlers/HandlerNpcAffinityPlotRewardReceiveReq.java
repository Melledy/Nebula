package emu.nebula.server.handlers;

import emu.nebula.data.GameData;
import emu.nebula.net.GameSession;
import emu.nebula.net.HandlerId;
import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.NpcAffinityPlotRewardReceive.NPCAffinityPlotRewardReceiveResp;
import emu.nebula.proto.Public.ItemTpl;
import emu.nebula.proto.Public.UI32;

@HandlerId(NetMsgId.npc_affinity_plot_reward_receive_req)
public class HandlerNpcAffinityPlotRewardReceiveReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        // Parse request
        var req = UI32.parseFrom(message);
        int plotId = req.getValue();
        
        // Get plot data
        var plot = GameData.getNpcAffinityPlotDataTable().get(plotId);
        if (plot == null) {
            return session.encodeMsg(NetMsgId.npc_affinity_plot_reward_receive_failed_ack);
        }
        
        // Claim reward
        var change = session.getPlayer().getStarTowerManager().receiveNpcAffinityPlotReward(plotId);
        
        if (change == null) {
            return session.encodeMsg(NetMsgId.npc_affinity_plot_reward_receive_failed_ack);
        }
        
        // Build response
        var rsp = NPCAffinityPlotRewardReceiveResp.newInstance()
                .setChange(change.toProto())
                .addShow(ItemTpl.newInstance()
                        .setTid(plot.getItemId())
                        .setQty(plot.getItemQty()));
        
        // Encode and send
        return session.encodeMsg(NetMsgId.npc_affinity_plot_reward_receive_succeed_ack, rsp);
    }

}
