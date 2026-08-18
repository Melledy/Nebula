package emu.nebula.server.handlers;

import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.CharArchive.CharArchiveRewardReceiveReq;
import emu.nebula.net.HandlerId;
import emu.nebula.data.GameData;
import emu.nebula.net.GameSession;

@HandlerId(NetMsgId.char_archive_reward_receive_req)
public class HandlerCharArchiveRewardReceiveReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        // Parse request
        var req = CharArchiveRewardReceiveReq.parseFrom(message);
        
        // Get archive
        var archive = GameData.getCharacterArchiveDataTable().get(req.getArchiveId());

        if (archive == null) {
            // Archive not found
            return session.encodeMsg(NetMsgId.char_archive_reward_receive_failed_ack);
        }
        
        // Get character
        var character = session.getPlayer().getCharacters().getCharacterById(archive.getCharacterId());

        if (character == null) {
            // Character not found
            return session.encodeMsg(NetMsgId.char_archive_reward_receive_failed_ack);
        }
        
        // Get archive reward
        var change = character.recvArchiveReward(archive);

        if (change == null) {
            return session.encodeMsg(NetMsgId.char_archive_reward_receive_failed_ack);
        } 
        
        // Encode reward and send
        return session.encodeMsg(NetMsgId.char_archive_reward_receive_succeed_ack, change.toProto());
    }

}
