package emu.nebula.server.handlers;

import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.TowerVitaProjectUpdate.TowerVitaProjectUpdateRep;
import emu.nebula.net.HandlerId;
import emu.nebula.net.GameSession;

@HandlerId(NetMsgId.tower_vita_project_update_req)
public class HandlerTowerVitaProjectUpdateReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        // Parse request
        var req = TowerVitaProjectUpdateRep.parseFrom(message);

        // Sanity
        if (!req.hasProjects()) {
            return session.encodeMsg(NetMsgId.tower_vita_project_update_failed_ack);
        }

        // Update project
        var project = session.getPlayer().getStarTowerManager().updateVitaProject(req.getProjects());

        if (project == null) {
            return session.encodeMsg(NetMsgId.tower_vita_project_update_failed_ack);
        }

        // Build response
        var rsp = TowerVitaProjectUpdateRep.newInstance()
                .setProjects(project.toProto());

        // Encode and send
        return session.encodeMsg(NetMsgId.tower_vita_project_update_succeed_ack, rsp);
    }

}
