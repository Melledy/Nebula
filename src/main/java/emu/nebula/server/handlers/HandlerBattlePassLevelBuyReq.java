package emu.nebula.server.handlers;

import emu.nebula.data.GameData;
import emu.nebula.game.player.PlayerChangeInfo;
import emu.nebula.net.GameSession;
import emu.nebula.net.HandlerId;
import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.BattlePassLevelBuy.BattlePassLevelBuyResp;
import emu.nebula.proto.Public.ChangeInfo;
import emu.nebula.proto.Public.UI32;

@HandlerId(NetMsgId.battle_pass_level_buy_req)
public class HandlerBattlePassLevelBuyReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        if (!session.getPlayer().isBattlePassUnlocked()) {
            return session.encodeMsg(NetMsgId.battle_pass_level_buy_failed_ack);
        }

        // Parse request using UI32 (standard uint32 protobuf type)
        var req = UI32.parseFrom(message);
        int levelsToBuy = req.getValue();
        
        if (levelsToBuy < 1) {
            levelsToBuy = 1;
        }
        
        // Get battle pass
        var battlePass = session.getPlayer().getBattlePassManager().getBattlePass();
        
        // Calculate total cost and total exp
        int currentLevel = battlePass.getLevel();
        int totalGemCost = 0;
        int totalExpToAdd = 0;
        int affordableLevels = 0;
        int costItemId = 2; // Gems
        
        for (int i = 1; i <= levelsToBuy; i++) {
            int targetLevel = currentLevel + i;
            var levelData = GameData.getBattlePassLevelDataTable().get(targetLevel);
            
            if (levelData == null) {
                // Max level reached
                break;
            }
            
            int cost = levelData.getQty();
            int currentGems = session.getPlayer().getInventory().getResourceCount(costItemId);
            
            if (currentGems < totalGemCost + cost) {
                // Not enough gems for all levels
                break;
            }
            
            totalGemCost += cost;
            totalExpToAdd += levelData.getExp();
            affordableLevels++;
        }
        
        if (affordableLevels == 0) {
            return session.encodeMsg(NetMsgId.battle_pass_level_buy_failed_ack);
        }
        
        // Prepare change info
        var change = new PlayerChangeInfo();
        
        // Deduct gems (total cost)
        if (totalGemCost > 0) {
            var gemCost = session.getPlayer().getInventory().addItem(costItemId, -totalGemCost);
            if (gemCost != null) {
                change.add(gemCost);
            }
        }
        
        // Add all exp at once (preserves current exp)
        battlePass.addExp(totalExpToAdd);
        
        // Save to database
        battlePass.save();
        
        // Build response
        var rsp = BattlePassLevelBuyResp.newInstance()
                .setLevel(battlePass.getLevel());
        
        // Add change info (gem cost)
        if (!change.isEmpty()) {
            var changeProto = ChangeInfo.newInstance();
            for (var any : change.getList()) {
                changeProto.addProps(any);
            }
            rsp.setChange(changeProto);
        }

        session.getPlayer().queueBattlePassStateNotify();
        
        // Encode and send
        return session.encodeMsg(NetMsgId.battle_pass_level_buy_succeed_ack, rsp);
    }

}
