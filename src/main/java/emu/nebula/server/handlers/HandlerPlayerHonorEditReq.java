package emu.nebula.server.handlers;

import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.Notify.HonorChangeNotify;
import emu.nebula.proto.PlayerHonorEdit.PlayerHonorEditReq;
import emu.nebula.proto.Public.HonorInfo;
import emu.nebula.proto.Public.Nil;
import emu.nebula.net.HandlerId;
import emu.nebula.net.GameSession;

@HandlerId(NetMsgId.player_honor_edit_req)
public class HandlerPlayerHonorEditReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        // Parse req
        var req = PlayerHonorEditReq.parseFrom(message);
        
        // Set
        boolean success = session.getPlayer().setHonor(req.getList());
        
        if (!success) {
            return session.encodeMsg(NetMsgId.player_honor_edit_failed_ack);
        }
        
        // Send notify message
        var proto = HonorChangeNotify.newInstance();
        
        for (int id : session.getPlayer().getHonor()) {
            proto.addHonors(HonorInfo.newInstance().setId(id));
        }
        
        session.getPlayer().addNextPackage(
            NetMsgId.honor_change_notify,
            proto
        );
        
        // Encode and send
        return session.encodeMsg(NetMsgId.player_honor_edit_succeed_ack, Nil.newInstance());
    }

}
