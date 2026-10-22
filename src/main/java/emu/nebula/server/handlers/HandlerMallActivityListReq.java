package emu.nebula.server.handlers;

import emu.nebula.data.GameData;
import emu.nebula.game.activity.type.MallLoginActivity;
import emu.nebula.net.GameSession;
import emu.nebula.net.HandlerId;
import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.MallActivityListOuterClass.MallActivityInfo;
import emu.nebula.proto.MallActivityListOuterClass.MallActivityList;

@HandlerId(NetMsgId.mall_activity_list_req)
public class HandlerMallActivityListReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        // Get player
        var player = session.getPlayer();
        
        // Build response
        var rsp = MallActivityList.newInstance();
        
        for (var data : GameData.getMallActivityDataTable()) {
            // Get the activity that this product unlocks
            var activity = player.getActivityManager().getActivity(MallLoginActivity.class, data.getActivityId());
            
            if (activity == null) {
                continue;
            }
            
            // Create proto info
            var info = MallActivityInfo.newInstance()
                    .setId(data.getIdString())
                    .setPurchased(activity.isActive());
            
            rsp.addList(info);
        }
        
        // Encode and send
        return session.encodeMsg(NetMsgId.mall_activity_list_succeed_ack, rsp);
    }

}
