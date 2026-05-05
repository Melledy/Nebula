package emu.nebula.server.handlers;

import emu.nebula.Nebula;
import emu.nebula.data.GameData;
import emu.nebula.game.gacha.GachaBannerInfo;
import emu.nebula.net.GameSession;
import emu.nebula.net.HandlerId;
import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.GachaInformation.GachaInformationResp;

@HandlerId(NetMsgId.gacha_information_req)
public class HandlerGachaInformationReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        // Build response
        var rsp = GachaInformationResp.newInstance();
        var manager = session.getPlayer().getGachaManager();
        synchronized (manager) {
            for (var data : GameData.getGachaDataTable().values()) {
                if (!data.isActiveAt(Nebula.getCurrentServerTime())) {
                    continue;
                }

                var bannerInfo = manager.findBannerInfo(data.getId());
                if (bannerInfo == null) {
                    bannerInfo = new GachaBannerInfo(data);
                }

                var pityState = manager.findPityState(data.getStorageId());
                rsp.addInformation(bannerInfo.toProto(pityState));
            }
        }
        
        // Encode and send
        return session.encodeMsg(NetMsgId.gacha_information_succeed_ack, rsp);
    }

}
