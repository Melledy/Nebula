package emu.nebula.server.handlers;

import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.TowerVitaProjectSet.TowerVitaProjectSetRep;
import emu.nebula.net.HandlerId;
import emu.nebula.net.GameSession;

@HandlerId(NetMsgId.tower_vita_project_set_req)
public class HandlerTowerVitaProjectSetReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        // Parse request
        var req = TowerVitaProjectSetRep.parseFrom(message);

        // Sanity
        if (!session.getPlayer().getStarTowerManager().selectVitaProject(req.getId())) {
            return session.encodeMsg(NetMsgId.tower_vita_project_set_failed_ack);
        }

        // Encode and send
        return session.encodeMsg(NetMsgId.tower_vita_project_set_succeed_ack);
    }

}
