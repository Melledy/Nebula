package emu.nebula.server.handlers;

import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.ActivityTowerDefenseLevelSettle.ActivityTowerDefenseLevelSettleReq;
import emu.nebula.net.HandlerId;
import emu.nebula.Nebula;
import emu.nebula.game.activity.type.TowerDefenseActivity;
import emu.nebula.net.GameSession;

@HandlerId(NetMsgId.activity_tower_defense_level_settle_req)
public class HandlerActivityTowerDefenseLevelSettleReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        // Parse request proto
        var req = ActivityTowerDefenseLevelSettleReq.parseFrom(message);

        // Get activity
        var activity = session.getPlayer().getActivityManager().getActivity(TowerDefenseActivity.class, 102001);

        // Claim rewards
        var change = activity.claimReward((int)req.getLevelId());

        // Update completed stages
        activity.getCompletedStages().put(req.getLevelId(), req.getStar());

        // Save changes
        session.getPlayer().save();

        // Encode and send
        return session.encodeMsg(NetMsgId.activity_tower_defense_level_settle_succeed_ack, change.toProto());
    }

}
