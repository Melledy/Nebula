package emu.nebula.server.handlers;

import emu.nebula.GameConstants;
import emu.nebula.net.GameSession;
import emu.nebula.net.HandlerId;
import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.DormFriendLike.DormFriendLikeReq;

@HandlerId(NetMsgId.dorm_friend_like_req)
public class HandlerDormFriendLikeReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        // Parse request
        var req = DormFriendLikeReq.parseFrom(message);

        long friendId = req.getFriendId();

        if (friendId <= 0 || friendId > Integer.MAX_VALUE) {
            return session.encodeMsg(NetMsgId.dorm_friend_like_failed_ack);
        }

        int uid = (int) friendId;
        var player = session.getPlayer();
        var manager = player.getDormManager();

        // Friends only
        if (!player.getFriendList().getFriends().containsKey(uid)) {
            return session.encodeMsg(NetMsgId.dorm_friend_like_failed_ack);
        }

        // Daily limit and duplicate check
        if (manager.hasLikedFriend(uid) || manager.getLikedFriendUids().size() >= GameConstants.DORM_LIKE_LIMIT) {
            return session.encodeMsg(NetMsgId.dorm_friend_like_failed_ack);
        }

        manager.addLikedFriend(uid);

        // Increment the friend's like counter
        var dorm = session.getPlayer().getDormManager().getFriendDorm(uid);

        if (dorm != null) {
            dorm.addLike();
            dorm.save();
        }

        // Encode and send
        return session.encodeMsg(NetMsgId.dorm_friend_like_succeed_ack);
    }

}
