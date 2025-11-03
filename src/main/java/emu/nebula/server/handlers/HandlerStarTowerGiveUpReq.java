package emu.nebula.server.handlers;

import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.StarTowerGiveUp.StarTowerGiveUpResp;
import emu.nebula.net.HandlerId;
import emu.nebula.net.GameSession;

@HandlerId(NetMsgId.star_tower_give_up_req)
public class HandlerStarTowerGiveUpReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        var game = session.getPlayer().getStarTowerManager().endGame();
        
        if (game == null) {
            return session.encodeMsg(NetMsgId.star_tower_give_up_failed_ack);
        }
        
        // Build response
        var rsp = StarTowerGiveUpResp.newInstance()
                .setFloor(game.getFloor());
        
        rsp.getMutableChange();
        rsp.setBuild(game.getBuild().toProto());
        
        return session.encodeMsg(NetMsgId.star_tower_give_up_succeed_ack, rsp);
    }

}
