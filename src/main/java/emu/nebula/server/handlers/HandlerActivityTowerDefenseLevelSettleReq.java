package emu.nebula.server.handlers;

import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.ActivityTowerDefenseLevelSettle.ActivityTowerDefenseLevelSettleReq;
import emu.nebula.net.HandlerId;
import emu.nebula.game.activity.type.TowerDefenseActivity;
import emu.nebula.net.GameSession;

@HandlerId(NetMsgId.activity_tower_defense_level_settle_req)
public class HandlerActivityTowerDefenseLevelSettleReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        // Parse request proto
        var req = ActivityTowerDefenseLevelSettleReq.parseFrom(message);

        // Get activity
        var activity = session.getPlayer().getActivityManager().getFirstActivity(TowerDefenseActivity.class);
        
        if (activity == null) {
            return session.encodeMsg(NetMsgId.activity_tower_defense_level_settle_failed_ack);
        }

        // Only receive rewards if we havent completed the stage
        if (!activity.getCompletedStages().containsKey(req.getLevelId())) {
            // Claim rewards
            var change = activity.claimReward(req.getLevelId());
            
            // Update completed stages and save activity to the database
            activity.getCompletedStages().put(req.getLevelId(), req.getStar());
            activity.save();
            
            // Encode and send
            return session.encodeMsg(NetMsgId.activity_tower_defense_level_settle_succeed_ack, change.toProto());
        } else {
            return session.encodeMsg(NetMsgId.activity_tower_defense_level_settle_succeed_ack);
        }
    }

}
