package emu.nebula.server.handlers;

import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.MallGemListOuterClass.GemInfo;
import emu.nebula.proto.MallGemListOuterClass.MallGemList;
import emu.nebula.net.HandlerId;
import emu.nebula.data.GameData;
import emu.nebula.net.GameSession;

@HandlerId(NetMsgId.mall_gem_list_req)
public class HandlerMallGemListReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        var rsp = MallGemList.newInstance();
        
        for (var data : GameData.getMallGemDataTable()) {
            var info = GemInfo.newInstance()
                    .setId(data.getIdString())
                    .setMaiden(true);
            
            rsp.addList(info);
        }
        
        return session.encodeMsg(NetMsgId.mall_gem_list_succeed_ack, rsp);
    }

}
