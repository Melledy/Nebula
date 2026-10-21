package emu.nebula.server.handlers;

import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.TowerVitaProjectNameUpdate.TowerVitaProjectNameUpdateRep;
import emu.nebula.net.HandlerId;
import emu.nebula.net.GameSession;

@HandlerId(NetMsgId.tower_vita_project_name_update_req)
public class HandlerTowerVitaProjectNameUpdateReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        // Parse request
        var req = TowerVitaProjectNameUpdateRep.parseFrom(message);

        // Sanity
        if (!req.hasId()) {
            return session.encodeMsg(NetMsgId.tower_vita_project_name_update_failed_ack);
        }

        // Rename project
        var project = session.getPlayer().getStarTowerManager().renameVitaProject(req.getId(), req.getName());

        if (project == null) {
            return session.encodeMsg(NetMsgId.tower_vita_project_name_update_failed_ack);
        }

        // Encode and send
        return session.encodeMsg(NetMsgId.tower_vita_project_name_update_succeed_ack);
    }

}
