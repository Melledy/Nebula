package emu.nebula.server.handlers;

import emu.nebula.Nebula;
import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.BattlePassOrderCollect.BattlePassOrderCollectResp;
import emu.nebula.proto.Public.CollectResp;
import emu.nebula.net.HandlerId;
import emu.nebula.net.GameSession;

@HandlerId(NetMsgId.battle_pass_order_collect_req)
public class HandlerBattlePassOrderCollectReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        if (!session.getPlayer().isBattlePassUnlocked()) {
            return session.encodeMsg(NetMsgId.battle_pass_order_collect_failed_ack);
        }

        // Get battle pass
        var battlePass = session.getPlayer().getBattlePassManager().getBattlePass();

        var payModule = Nebula.getGameContext().getPayModule();
        var collectResult = payModule.consumePendingCollect(session);
        if (collectResult == null) {
            if (!battlePass.isPremium()) {
                return session.encodeMsg(NetMsgId.battle_pass_order_collect_failed_ack);
            }
            collectResult = emu.nebula.game.mall.MallOrderCollectResult.empty();
        }

        // Build collect response
        var collectResp = CollectResp.newInstance()
                .setStatusValue(1); // Success

        if (collectResult.hasDisplayChange()) {
            collectResp.setItems(collectResult.getDisplayChange());
        }

        if (collectResult.hasStateChange()) {
            payModule.pushInventoryNotifies(session, collectResult.getStateChange());
        }

        // Build response
        var rsp = BattlePassOrderCollectResp.newInstance()
                .setMode(battlePass.getMode())
                .setLevel(battlePass.getLevel())
                .setVersion(battlePass.getBattlePassId())
                .setCollectResp(collectResp);
        
        // Encode and send
        return session.encodeMsg(NetMsgId.battle_pass_order_collect_succeed_ack, rsp);
    }

}
