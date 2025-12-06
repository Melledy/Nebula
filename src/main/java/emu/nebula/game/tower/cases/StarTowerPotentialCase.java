package emu.nebula.game.tower.cases;

import java.util.List;

import emu.nebula.GameConstants;
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
        // Check 
        var select = req.getMutableSelectReq();
        
        if (select.hasReRoll()) {
            return this.reroll(rsp);
        } else {
            return this.select(select.getIndex(), rsp);
        }
    }
    
    private StarTowerInteractResp reroll(StarTowerInteractResp rsp) {
        // Check if we can reroll
        if (!this.canReroll()) {
            return rsp;
        }
        
        // Check price
        int coin = this.getGame().getResCount(GameConstants.TOWER_COIN_ITEM_ID);
        int price = this.getRerollPrice();
        
        if (coin < price) {
            return rsp;
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
            return rsp;
        }
        
        // Clear reroll count
        rerollCase.setReroll(newReroll);
        
        // Add reroll case
        this.getRoom().addCase(rsp.getMutableCases(), rerollCase);
        
        // Finish subtracting rerolls
        this.reroll = newReroll;
        
        // Subtract coins
        var change = this.getGame().addItem(GameConstants.TOWER_COIN_ITEM_ID, -price);
        
        rsp.setChange(change.toProto());
        
        // Complete
        return rsp;
    }
    
    private StarTowerInteractResp select(int index, StarTowerInteractResp rsp) {
        // Get selected potential
        var potential = this.selectId(index);
        if (potential == null) {
            return rsp;
        }
        
        // Add potential
        var change = this.getGame().addItem(potential.getId(), potential.getLevel());
        
        // Set change
        rsp.setChange(change.toProto());
        
        // Handle pending potential selectors
        var nextCases = this.getGame().handlePendingPotentialSelectors();
        
        for (var towerCase : nextCases) {
            this.getRoom().addCase(rsp.getMutableCases(), towerCase);
        }
        
        // Complete
        return rsp;
    }
    
    // Proto
    
    @Override
    public void encodeProto(StarTowerRoomCase proto) {
        var select = proto.getMutableSelectPotentialCase()
            .setTeamLevel(this.getTeamLevel());
        
        for (var potential : this.getPotentials()) {
            select.addInfos(potential.toProto());
        }
        
        if (this.canReroll()) {
            select.setCanReRoll(true);
            select.setReRollPrice(this.getRerollPrice());
        }
    }
}
