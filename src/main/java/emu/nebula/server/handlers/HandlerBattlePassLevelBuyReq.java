package emu.nebula.server.handlers;

import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.net.HandlerId;
import emu.nebula.net.GameSession;
import emu.nebula.proto.Public.UI32;
import emu.nebula.proto.BattlePassLevelBuy.BattlePassLevelBuyResp;

@HandlerId(NetMsgId.battle_pass_level_buy_req)
public class HandlerBattlePassLevelBuyReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        // Parse req
        var req = UI32.parseFrom(message);
        
        // Buy levels
        var battlePass = session.getPlayer().getBattlePassManager().getBattlePass().buyLevels(req.getValue());
        
        if (battlePass == null) {
            return session.encodeMsg(NetMsgId.battle_pass_level_buy_failed_ack);
        }
        
        // Build response
        var rsp = BattlePassLevelBuyResp.newInstance()
                .setLevel(battlePass.getLevel());
        
        // Encode and send
        return session.encodeMsg(NetMsgId.battle_pass_level_buy_succeed_ack, rsp);
    }

}
