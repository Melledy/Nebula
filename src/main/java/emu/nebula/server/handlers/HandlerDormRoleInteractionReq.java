package emu.nebula.server.handlers;

import emu.nebula.net.GameSession;
import emu.nebula.net.HandlerId;
import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.DormRoleInteraction.DormRoleInteractionReq;
import emu.nebula.proto.DormRoleInteraction.DormRoleInteractionResp;
import emu.nebula.proto.Public;
import emu.nebula.proto.PublicDorm.AffinityData;

@HandlerId(NetMsgId.dorm_role_interaction_req)
public class HandlerDormRoleInteractionReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        // Parse request
        var req = DormRoleInteractionReq.parseFrom(message);

        // Interact with roles
        var result = session.getPlayer().getDormManager().interactRoles(req.getRoleInteractions());
        
        if (result == null) {
            return session.encodeMsg(NetMsgId.dorm_role_interaction_failed_ack);
        }

        // Build response
        var rsp = DormRoleInteractionResp.newInstance()
                .setChange(result.getChange().toProto());

        for (var interaction : result.getInteractions()) {
            int itemId = interaction.itemId();

            var itemInfo = Public.ItemTpl.newInstance()
                    .setTid(Math.max(itemId, 0))
                    .setQty(itemId > 0 ? 1 : 0);

            rsp.addInfoList(
                    AffinityData.newInstance()
                            .setAffinityInfo(interaction.character().getAffinityProto())
                            .setItemInfo(itemInfo)
            );
        }

        // Encode and send
        return session.encodeMsg(NetMsgId.dorm_role_interaction_succeed_ack, rsp);
    }

}
