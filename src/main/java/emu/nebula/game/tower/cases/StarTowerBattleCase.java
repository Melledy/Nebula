package emu.nebula.game.tower.cases;

import emu.nebula.GameConstants;
import emu.nebula.data.GameData;
import emu.nebula.game.player.PlayerChangeInfo;
import emu.nebula.game.tower.room.StarTowerBattleRoom;
import emu.nebula.proto.PublicStarTower.StarTowerRoomCase;
import emu.nebula.proto.StarTowerInteract.StarTowerInteractReq;
import emu.nebula.proto.StarTowerInteract.StarTowerInteractResp;
import lombok.Getter;

@Getter
public class StarTowerBattleCase extends StarTowerBaseCase {
    private int subNoteSkillNum;
    
    public StarTowerBattleCase() {
        this(0);
    }
    
    public StarTowerBattleCase(int subNoteSkillNum) {
        this.subNoteSkillNum = subNoteSkillNum;
    }

    @Override
    public CaseType getType() {
        return CaseType.Battle;
    }

    @Override
    public StarTowerInteractResp interact(StarTowerInteractReq req, StarTowerInteractResp rsp) {
        // Parse battle end
        var proto = req.getBattleEndReq();
        
        // Init change
        var change = new PlayerChangeInfo();
        
        // Handle victory/defeat
        if (proto.hasVictory()) {
            // Handle leveling up

            // Get relevant floor exp data
            // fishiatee: THERE'S NO LINQ IN JAVAAAAAAAAAAAAA
            var floorExpData = GameData.getStarTowerFloorExpDataTable().stream()
                                .filter(f -> f.getStarTowerId() == this.getGame().getId())
                                .findFirst()
                                .orElseThrow();
            int expReward = 0;

            // Determine appropriate exp reward
            switch (this.getRoom().getType()) {
                // Regular battle room
                case 0:
                    expReward = floorExpData.getNormalExp();
                    break;
                // Elite battle room
                case 1:
                    expReward = floorExpData.getEliteExp();
                    break;
                // Non-final boss room
                case 2:
                    expReward = floorExpData.getBossExp();
                    break;
                // Final room
                case 3:
                    expReward = floorExpData.getFinalBossExp();
                    break;
            }

            // Level up
            this.getGame().addExp(expReward);
            this.getGame().addPotentialSelectors(this.getGame().levelUp());
            
            // Add clear time
            this.getGame().addBattleTime(proto.getVictory().getTime());
            
            // Handle victory
            rsp.getMutableBattleEndResp()
                .getMutableVictory()
                .setLv(this.getGame().getTeamLevel())
                .setBattleTime(this.getGame().getBattleTime());
            
            // Add money
            int money = this.getRoom().getStage().getInteriorCurrencyQuantity();
            
            this.getGame().addItem(GameConstants.STAR_TOWER_GOLD_ITEM_ID, money, change);
            
            // Handle pending potential selectors
            var potentialCase = this.getGame().handlePendingPotentialSelectors();
            if (potentialCase != null) {
                // Create potential selector
                this.getGame().addCase(rsp.getMutableCases(), potentialCase);
            } else if (!this.getRoom().hasDoor()) {
                // Add door case here if door hasn't opened yet
                this.getGame().createExit(rsp.getMutableCases());
                // Create recovery npc
                if (this.getRoom() instanceof StarTowerBattleRoom) {
                    this.getRoom().addCase(rsp.getMutableCases(), new StarTowerNpcRecoveryHPCase());
                }
            }
            
            // Add sub note skills
            this.getGame().addRandomSubNoteSkills(this.getGame().getPendingSubNotes(), change);

            // Handle client events for achievements
            this.getGame().getPlayer().getAchievementManager().handleClientEvents(proto.getVictory().getEvents());
        } else {
            // Handle defeat
            // TODO
            return this.getGame().settle(rsp, false);
        }
        
        // Set change
        rsp.setChange(change.toProto());
        
        // Return response for the player
        return rsp;
    }
    
    // Proto
    
    @Override
    public void encodeProto(StarTowerRoomCase proto) {
        proto.getMutableBattleCase()
            .setSubNoteSkillNum(this.getSubNoteSkillNum());
    }
}
