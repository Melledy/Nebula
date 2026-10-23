package emu.nebula.server.handlers;

import emu.nebula.net.GameSession;
import emu.nebula.net.HandlerId;
import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.BattlePassLevelBuy.BattlePassLevelBuyResp;
import emu.nebula.proto.Public.UI32;

@HandlerId(NetMsgId.battle_pass_level_buy_req)
public class HandlerBattlePassLevelBuyReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        // Sanity check to make sure battlepass is unlocked
        if (!session.getPlayer().getBattlePassManager().isUnlocked()) {
            return session.encodeMsg(NetMsgId.battle_pass_level_buy_failed_ack);
        }

        // Parse request
        var req = UI32.parseFrom(message);
        
        // Get battlepass
        var battlePass = session.getPlayer().getBattlePassManager().getBattlePass();
        
        // Buy levels
        var change = battlePass.buyLevels(req.getValue());
        
        // Check
        if (change == null) {
            return session.encodeMsg(NetMsgId.battle_pass_level_buy_failed_ack);
        }
        
        // Build response
        var rsp = BattlePassLevelBuyResp.newInstance()
                .setLevel(battlePass.getLevel())
                .setChange(change.toProto());

        session.getPlayer().getBattlePassManager().updateState();
        
        // Encode and send
        return session.encodeMsg(NetMsgId.battle_pass_level_buy_succeed_ack, rsp);
    }

}
