package emu.nebula.game.tower.cases;

import emu.nebula.GameConstants;
import emu.nebula.game.quest.QuestCondition;
import emu.nebula.proto.PublicStarTower.StarTowerRoomCase;
import emu.nebula.proto.StarTowerInteract.StarTowerInteractReq;
import emu.nebula.proto.StarTowerInteract.StarTowerInteractResp;

import lombok.Getter;

@Getter
public class StarTowerStrengthenMachineCase extends StarTowerBaseCase {
    private boolean free;
    private int discount;
    private int times;
    
    @Override
    public boolean removeAfterInteract() {
        return false;
    }
    
    @Override
    public void onRegister() {
        // Set strengthen price
        this.free = this.getModifiers().isFreeStrengthen();
        this.discount = this.getModifiers().getStrengthenDiscount();
    }
    
    public int getPrice() {
        if (this.free) {
            return 0;
        }
        
        int price = 120 + (this.times * 60) - this.discount;
        
        return Math.max(price, 0);
    }
    
    public void increasePrice() {
        if (this.free) {
            this.free = false;
            this.getModifiers().setFreeStrengthen(false);
        } else {
            this.times++;
        }
    }

    @Override
    public CaseType getType() {
        return CaseType.StrengthenMachine;
    }
    
    @Override
    public StarTowerInteractResp interact(StarTowerInteractReq req, StarTowerInteractResp rsp) {
        // Init case
        StarTowerBaseCase towerCase = null;
        
        // Check coin
        int coin = getGame().getResCount(GameConstants.TOWER_COIN_ITEM_ID);
        int price = this.getPrice();
        
        if (coin >= price) {
            towerCase = getGame().createStrengthenSelector();
        }
        
        if (towerCase != null) {
            // Add enhancement selector case
            this.getRoom().addCase(rsp.getMutableCases(), towerCase);
            
            // Remove coins
            var change = this.getGame().addItem(GameConstants.TOWER_COIN_ITEM_ID, -price);
            
            // Set change info
            rsp.setChange(change.toProto());
            
            // Increment price
            this.increasePrice();
            
            // Trigger quests + achievements
            this.getGame().getPlayer().trigger(QuestCondition.TowerSpecificDifficultyStrengthenMachineTotal, 1, 0);
        }
        
        // Set success result
        rsp.getMutableStrengthenMachineResp()
            .setBuySucceed(towerCase != null);
        
        // Complete
        return rsp;
    }
    
    // Proto
    
    @Override
    public void encodeProto(StarTowerRoomCase proto) {
        // Set field in the proto
        proto.getMutableStrengthenMachineCase()
            .setFirstFree(this.isFree())
            .setDiscount(this.getDiscount())
            .setTimes(this.getTimes());
    }
}
