package emu.nebula.server.handlers;

import emu.nebula.net.GameSession;
import emu.nebula.net.HandlerId;
import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.DormFriendList.DormFriendListResp;
import emu.nebula.proto.PublicDorm.DormFriendDetail;

@HandlerId(NetMsgId.dorm_friend_list_req)
public class HandlerDormFriendListReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        var player = session.getPlayer();
        var manager = player.getDormManager();

        var rsp = DormFriendListResp.newInstance();

        for (var friendship : player.getFriendList().getFriends().values()) {
            int uid = friendship.getFriendUid();

            var dorm = session.getPlayer().getDormManager().getFriendDorm(uid);

            var detail = DormFriendDetail.newInstance()
                    .setUid(uid)
                    .setLikeNum(dorm == null ? 0 : dorm.getLikeNum())
                    .setCollectionValue(dorm == null ? 0 : dorm.getCollectionValue())
                    .setIsLike(manager.hasLikedFriend(uid));

            if (dorm != null && dorm.getDormName() != null) {
                detail.setDormName(dorm.getDormName());
            }

            rsp.addFriends(detail);
        }

        // Encode and send
        return session.encodeMsg(NetMsgId.dorm_friend_list_succeed_ack, rsp);
    }

}
