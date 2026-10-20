package emu.nebula.server.handlers;

import emu.nebula.game.inventory.ItemParamMap;
import emu.nebula.net.GameSession;
import emu.nebula.net.HandlerId;
import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.DormAffinityGiftSend.DormAffinityGiftSendReq;
import emu.nebula.proto.DormAffinityGiftSend.DormAffinityGiftSendResp;

@HandlerId(NetMsgId.dorm_affinity_gift_send_req)
public class HandlerDormAffinityGiftSendReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        // Parse request
        var req = DormAffinityGiftSendReq.parseFrom(message);

        var player = session.getPlayer();
        var manager = player.getDormManager();

        // Get character
        var character = player.getCharacters().getCharacterById(req.getCharId());
        if (character == null) {
            return session.encodeMsg(NetMsgId.dorm_affinity_gift_send_failed_ack);
        }

        // Parse gift items
        var items = ItemParamMap.fromTemplates(req.getItems());

        // Send gifts
        var change = manager.sendAffinityGift(character, req.getDormRoomId(), items);
        if (change == null) {
            return session.encodeMsg(NetMsgId.dorm_affinity_gift_send_failed_ack);
        }

        // Build response
        var rsp = DormAffinityGiftSendResp.newInstance()
                .setInfo(character.getAffinityProto())
                .setChange(change.toProto())
                .setSendGiftCnt(manager.getDailyGiftCount(character.getCharId()));

        // Encode and send
        return session.encodeMsg(NetMsgId.dorm_affinity_gift_send_succeed_ack, rsp);
    }

}
