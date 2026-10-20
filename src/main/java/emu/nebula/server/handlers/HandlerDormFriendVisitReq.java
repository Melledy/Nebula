package emu.nebula.server.handlers;

import emu.nebula.net.GameSession;
import emu.nebula.net.HandlerId;
import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.DormFriendVisit.DormFriendVisitReq;
import emu.nebula.proto.DormFriendVisit.DormFriendVisitResp;
import emu.nebula.proto.PublicDorm.DormFriendInfo;

@HandlerId(NetMsgId.dorm_friend_visit_req)
public class HandlerDormFriendVisitReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        // Parse request
        var req = DormFriendVisitReq.parseFrom(message);

        // Validate friend
        long friendId = req.getFriendId();

        if (friendId <= 0 || friendId > Integer.MAX_VALUE) {
            return session.encodeMsg(NetMsgId.dorm_friend_visit_failed_ack);
        }

        int uid = (int) friendId;

        if (!session.getPlayer().getFriendList().getFriends().containsKey(uid)) {
            return session.encodeMsg(NetMsgId.dorm_friend_visit_failed_ack);
        }

        // Load friend dorm data
        var dorm = session.getPlayer().getDormManager().getFriendDorm(uid);

        if (dorm == null) {
            return session.encodeMsg(NetMsgId.dorm_friend_visit_failed_ack);
        }
        
        // Build response
        var rsp = DormFriendVisitResp.newInstance();

        var unlockedRoomIds = dorm.getUnlockedRoomIds();
        var sortedRoomIds = new java.util.ArrayList<>(unlockedRoomIds);
        sortedRoomIds.sort(Integer::compareTo);

        for (int roomId : sortedRoomIds) {
            var room = dorm.getOrCreateRoom(roomId);

            if (room != null) {
                rsp.addDormList(room.toProto());
            }
        }

        var dormInfo = DormFriendInfo.newInstance();

        if (dorm.getDormName() != null) {
            dormInfo.setDormName(dorm.getDormName());
        }

        rsp.setDormInfo(dormInfo);

        // Record the visit on the friend's dorm
        dorm.recordVisit(session.getPlayer().getUid());
        dorm.save();

        // Encode and send
        return session.encodeMsg(NetMsgId.dorm_friend_visit_succeed_ack, rsp);
    }

}
