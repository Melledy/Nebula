package emu.nebula.server.handlers;

import java.util.ArrayList;
import java.util.Comparator;

import emu.nebula.GameConstants;
import emu.nebula.game.dorm.DormPreset;
import emu.nebula.net.GameSession;
import emu.nebula.net.HandlerId;
import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.DormFurniturePreset.DormFurniturePresetListResp;

@HandlerId(NetMsgId.dorm_furniture_preset_list_req)
public class HandlerDormFurniturePresetListReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        // Get player dorm
        var dorm = session.getPlayer().getDormManager();
        
        // Build response
        var rsp = DormFurniturePresetListResp.newInstance()
                .setNum(GameConstants.DORM_DEFAULT_PRESET_NUM);

        // Get preset list
        var list = new ArrayList<>(dorm.getPresets().values());
        
        // Sort preset list
        list.sort(Comparator.comparingInt(DormPreset::getBuildId));

        // Encode presets to proto
        for (var preset : list) {
            rsp.addPresetList(preset.toProto());
        }

        // Encode and send
        return session.encodeMsg(NetMsgId.dorm_furniture_preset_list_succeed_ack, rsp);
    }

}
