package emu.nebula.server.handlers;

import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.ActivityPenguinCardLevelSettle.ActivityPenguinCardSettleReq;
import it.unimi.dsi.fastutil.ints.IntList;
import emu.nebula.net.HandlerId;
import emu.nebula.game.activity.type.PenguinCardActivity;
import emu.nebula.game.activity.type.PenguinCardActivity.LevelStats;
import emu.nebula.net.GameSession;

@HandlerId(NetMsgId.activity_penguin_card_level_settle_req)
public class HandlerActivityPenguinCardSettleReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        // Parse request proto
        var req = ActivityPenguinCardSettleReq.parseFrom(message);

        // Get activity
        var activity = session.getPlayer().getActivityManager().getActivity(PenguinCardActivity.class, 800001);

        // Update completed levels
        LevelStats data = new LevelStats();
        data.setScore(req.getScore());
        data.setStars(req.getStar());
        activity.getCompletedLevels().put(req.getLevelId(), data);

        // Encode and send
        return session.encodeMsg(NetMsgId.activity_penguin_card_level_settle_succeed_ack);
    }

}
