package emu.nebula.server.handlers;

import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.TowerVitaProjectList.TowerVitaProjectListResp;
import emu.nebula.net.HandlerId;
import emu.nebula.net.GameSession;

@HandlerId(NetMsgId.tower_vita_project_list_req)
public class HandlerTowerVitaProjectListReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        // Get manager
        var manager = session.getPlayer().getStarTowerManager();

        // Build response
        var rsp = TowerVitaProjectListResp.newInstance()
                .setUseId(manager.getVitaData().getSelectedProjectId());

        // Add projects
        for (var project : manager.getVitaProjects()) {
            rsp.addProjects(project.toProto());
        }

        // Encode and send
        return session.encodeMsg(NetMsgId.tower_vita_project_list_succeed_ack, rsp);
    }

}
