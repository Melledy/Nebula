package emu.nebula.server.handlers;

import emu.nebula.Nebula;
import emu.nebula.data.GameData;
import emu.nebula.net.GameSession;
import emu.nebula.net.HandlerId;
import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.BattlePassOrder.BattlePassOrderReq;

@HandlerId(NetMsgId.battle_pass_order_req)
public class HandlerBattlePassOrderReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        // Parse request
        var req = BattlePassOrderReq.parseFrom(message);
        // Validate mode (1 = Premium, 2 = Luxury)
        int requestedMode = req.getMode();
        if (!session.getPlayer().isBattlePassUnlocked() || requestedMode < 1 || requestedMode > 2) {
            return session.encodeMsg(NetMsgId.battle_pass_order_failed_ack);
        }
        
        // Get battle pass
        var battlePass = session.getPlayer().getBattlePassManager().getBattlePass();

        // Allow: 0->1 (free to premium), 0->2 (free to luxury), 1->2 (premium to luxury)
        int currentMode = battlePass.getMode();
        if (requestedMode <= currentMode) {
            return session.encodeMsg(NetMsgId.battle_pass_order_failed_ack);
        }

        var data = GameData.getBattlePassDataTable().get(battlePass.getBattlePassId());
        if (data == null) {
            return session.encodeMsg(NetMsgId.battle_pass_order_failed_ack);
        }
        
        // Follow the same fake-payment lifecycle as mall orders:
        // order ack -> paid notify -> order collect -> rewards popup.
        return session.encodeMsg(
                NetMsgId.battle_pass_order_succeed_ack,
                Nebula.getGameContext().getPayModule().createBattlePassOrder(session, data, requestedMode)
        );
    }

}
