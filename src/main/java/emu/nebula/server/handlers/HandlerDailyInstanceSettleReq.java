package emu.nebula.server.handlers;

import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.DailyInstanceSettle.DailyInstanceSettleReq;
import emu.nebula.proto.DailyInstanceSettle.DailyInstanceSettleResp;
import emu.nebula.net.HandlerId;
import emu.nebula.data.GameData;
import emu.nebula.game.instance.InstanceSettleData;
import emu.nebula.net.GameSession;

@HandlerId(NetMsgId.daily_instance_settle_req)
public class HandlerDailyInstanceSettleReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        // Cache player
        var player = session.getPlayer();
        
        // Get boss level data
        var data = GameData.getDailyInstanceDataTable().get(player.getInstanceManager().getCurInstanceId());
        if (data == null || !data.hasEnergy(player)) {
            return this.encodeMsg(NetMsgId.daily_instance_settle_failed_ack);
        }
        
        // Parse request
        var req = DailyInstanceSettleReq.parseFrom(message);
        
        // Settle instance
        var changes = player.getInstanceManager().settleInstance(
                data,
                player.getInstanceManager().getDailyInstanceLog(),
                "dailyInstanceLog",
                req.getStar()
        );
        
        var settleData = (InstanceSettleData) changes.getExtraData();
        
        // Create response
        var rsp = DailyInstanceSettleResp.newInstance()
                .setExp(settleData.getExp())
                .setChange(changes.toProto());
        
        // Add reward items
        if (settleData.isWin()) {
            int rewardType = player.getInstanceManager().getRewardType();
            
            data.getRewards(rewardType).toItemProtoStream().forEach(rsp::addSelect);
            
            if (settleData.isFirst()) {
                data.getFirstRewards(rewardType).toItemProtoStream().forEach(rsp::addFirst);
            }
        }
        
        // Send response
        return this.encodeMsg(NetMsgId.daily_instance_settle_succeed_ack, rsp);
    }

}
