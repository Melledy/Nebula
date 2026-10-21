package emu.nebula.server.handlers;

import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.Public.UI32;
import emu.nebula.proto.TowerVitaEnergyBuy.TowerVitaEnergyBuyResp;
import emu.nebula.net.HandlerId;
import emu.nebula.net.GameSession;

@HandlerId(NetMsgId.tower_vita_energy_buy_req)
public class HandlerTowerVitaEnergyBuyReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        // Parse request
        var req = UI32.parseFrom(message);

        // Get vita data
        var data = session.getPlayer().getStarTowerManager().getVitaData();

        // Buy energy
        var change = data.buyEnergy(req.getValue());

        if (change == null) {
            return session.encodeMsg(NetMsgId.tower_vita_energy_buy_failed_ack);
        }

        // Build response
        var rsp = TowerVitaEnergyBuyResp.newInstance()
                .setEnergy(data.getEnergy())
                .setChange(change.toProto());

        // Encode and send
        return session.encodeMsg(NetMsgId.tower_vita_energy_buy_succeed_ack, rsp);
    }

}
