package emu.nebula.game.tower.cases;

import java.util.List;

import emu.nebula.GameConstants;
import emu.nebula.game.quest.QuestCondition;
import emu.nebula.game.tower.StarTowerGame;
import emu.nebula.game.tower.StarTowerPotentialInfo;
import emu.nebula.proto.PublicStarTower.StarTowerRoomCase;
import emu.nebula.proto.StarTowerInteract.StarTowerInteractReq;
import emu.nebula.proto.StarTowerInteract.StarTowerInteractResp;

import lombok.Getter;

@Getter
public class StarTowerPotentialCase extends StarTowerBaseCase {
    private int teamLevel;
    private int charId;
    private int reroll;
    private int rerollPrice;
    private boolean strengthen;
    private List<StarTowerPotentialInfo> potentials;
    
    public StarTowerPotentialCase(StarTowerGame game, boolean strengthen, List<StarTowerPotentialInfo> potentials) {
        this(game, 0, potentials);
        this.strengthen = strengthen;
    }
    
    public StarTowerPotentialCase(StarTowerGame game, int charId, List<StarTowerPotentialInfo> potentials) {
        this.teamLevel = game.getTeamLevel();
        this.charId = charId;
        this.reroll = game.getModifiers().getPotentialRerollCount();
        this.rerollPrice = 100 - game.getModifiers().getPotentialRerollDiscount();
        this.potentials = potentials;
    }

    @Override
    public CaseType getType() {
        return CaseType.PotentialSelect;
    }
    
    public boolean isRare() {
        return false;
    }
    
    public void setReroll(int count) {
        this.reroll = count;
    }
    
    public boolean canReroll() {
        return this.reroll > 0;
    }
    
    public StarTowerPotentialInfo selectId(int index) {
        if (index < 0 || index >= this.getPotentials().size()) {
            return null;
        }
        
        return this.getPotentials().get(index);
    }
    
    @Override
    public StarTowerInteractResp interact(StarTowerInteractReq req, StarTowerInteractResp rsp) {
        // Get select req
        var select = req.getMutableSelectReq();
        
        // Handle select option
        if (select.hasReRoll()) {
            this.reroll(select.getAuto(), rsp);
        } else {
            this.select(select.getIndex(), select.getAuto(), rsp);
        }
        
        return rsp;
    }
    
    private void reroll(boolean auto, StarTowerInteractResp rsp) {
        // Check if we can reroll
        if (!this.canReroll()) {
            return;
        }
        
        // Check price
        int coin = this.getGame().getResCount(GameConstants.TOWER_COIN_ITEM_ID);
        int price = this.getRerollPrice();
        
        if (coin < price) {
            return;
        }
        
        // Subtract rerolls
        int newReroll = this.reroll - 1;
        
        // Create reroll case
        StarTowerPotentialCase rerollCase = null;
        
        if (this.isStrengthen()) {
            rerollCase = this.getGame().createStrengthenSelector();
        } else {
            rerollCase = this.getGame().createPotentialSelector(this.getCharId(), this.isRare());
        }
        
        if (rerollCase == null) {
            return;
        }
        
        // Clear reroll count
        rerollCase.setReroll(newReroll);
        
        // Add reroll case
        this.getRoom().addCase(rsp.getMutableCases(), rerollCase);
        
        // Finish subtracting rerolls
        this.reroll = newReroll;
        
        // Subtract coins
        var change = this.getGame().addItem(GameConstants.TOWER_COIN_ITEM_ID, -price);
        
        // Auto mode consumes vita energy
        if (auto) {
            this.getPlayer().getInventory().removeItem(GameConstants.VITA_ENERGY_ITEM_ID, GameConstants.VITA_ENERGY_COST, change);
        }
        
        // Set change proto in response
        rsp.setChange(change.toProto());
        
        // Achievement + Star Tower quests
        this.getGame().getPlayer().trigger(QuestCondition.TowerSpecificPotentialReRollTotal, 1);
    }
    
    private void select(int index, boolean auto, StarTowerInteractResp rsp) {
        // Get selected potential
        var potential = this.selectId(index);
        if (potential == null) {
            return;
        }
        
        // Achievement
        this.triggerBonusLevelAchievement(potential);
        
        // Add potential
        var change = this.getGame().addItem(potential.getId(), potential.getLevel());
        
        // Auto mode consumes vita energy
        if (auto) {
            this.getPlayer().getInventory().removeItem(GameConstants.VITA_ENERGY_ITEM_ID, GameConstants.VITA_ENERGY_COST, change);
        }
        
        // Set change proto in response
        rsp.setChange(change.toProto());
        
        // Handle pending potential selectors
        var nextCases = this.getGame().handlePendingPotentialSelectors();
        
        for (var towerCase : nextCases) {
            this.getRoom().addCase(rsp.getMutableCases(), towerCase);
        }
    }
    
    private void triggerBonusLevelAchievement(StarTowerPotentialInfo potential) {
        // Check if potential is lucky (+! or more levels)
        if (potential.getLevel() <= 1) {
            return;
        }
        
        if (this.getGame().getPotentials().containsKey(potential.getId())) {
            // Enhancing potentials
            this.getGame().getPlayer().trigger(QuestCondition.TowerSpecificPotentialLuckyTotal, 1);
        } else {
            // Adding new potential
            this.getGame().getPlayer().trigger(QuestCondition.TowerSpecificPotentialBonusTotal, 1);
        }
    }
    
    // Proto
    
    @Override
    public void encodeProto(StarTowerRoomCase proto) {
        var select = proto.getMutableSelectPotentialCase()
            .setTeamLevel(this.getTeamLevel());
        
        for (var potential : this.getPotentials()) {
            select.addInfos(potential.toProto());
            
            if (potential.getLevel() > 1) {
                select.addLuckyIds(potential.getId());
            }
            
            if (this.getGame().isNewPotential(potential.getId())) {
                select.addNewIds(potential.getId());
            }
        }
        
        if (this.canReroll()) {
            select.setCanReRoll(true);
            select.setReRollPrice(this.getRerollPrice());
        }
    }
}
