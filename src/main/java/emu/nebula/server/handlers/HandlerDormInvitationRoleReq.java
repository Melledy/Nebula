package emu.nebula.server.handlers;

import java.util.ArrayList;

import emu.nebula.net.GameSession;
import emu.nebula.net.HandlerId;
import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.DormInvitationRole.DormInvitationRoleReq;
import emu.nebula.proto.Public;

@HandlerId(NetMsgId.dorm_invitation_role_req)
public class HandlerDormInvitationRoleReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        // Parse request
        var req = DormInvitationRoleReq.parseFrom(message);

        // Get role ids
        var roleIds = new ArrayList<Integer>();
        for (var info : req.getInvitationRoleList()) {
            roleIds.add(info.getRoleId());
        }

        // Update invitation roles
        if (!session.getPlayer().getDormManager().updateInvitationRoles(req.getDormRoomId(), roleIds)) {
            return session.encodeMsg(NetMsgId.dorm_invitation_role_failed_ack);
        }

        // Encode and send
        return session.encodeMsg(NetMsgId.dorm_invitation_role_succeed_ack, Public.Nil.newInstance());
    }

}
