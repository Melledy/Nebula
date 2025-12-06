package emu.nebula.game.tower.cases;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import emu.nebula.GameConstants;
import emu.nebula.game.player.PlayerChangeInfo;
import emu.nebula.game.tower.StarTowerShopGoods;
import emu.nebula.proto.PublicStarTower.HawkerCaseData;
import emu.nebula.proto.PublicStarTower.HawkerGoods;
import emu.nebula.proto.PublicStarTower.StarTowerRoomCase;
import emu.nebula.proto.StarTowerInteract.StarTowerInteractReq;
import emu.nebula.proto.StarTowerInteract.StarTowerInteractResp;
import emu.nebula.util.Utils;

import lombok.Getter;

@Getter
public class StarTowerHawkerCase extends StarTowerBaseCase {
    private Map<Integer, StarTowerShopGoods> goods;
    
    public StarTowerHawkerCase() {
        this.goods = new HashMap<>();
    }

    @Override
    public CaseType getType() {
        return CaseType.Hawker;
    }
    
    @Override
    public void onRegister() {
        this.initGoods();
    }
    
    public void initGoods() {
        // Clear goods
        this.getGoods().clear();
        
        // Caclulate amount of potentials/sub notes to sell
        int total = getModifiers().getShopGoodsCount();
        
        int minPotentials = Math.max(total / 2, 2);
        int maxPotentials = Math.max(total - 1, minPotentials);
        int potentials = Utils.randomRange(minPotentials, maxPotentials);
        
        int subNotes = total - potentials;
        boolean hasCoins = this.getGame().getResCount(GameConstants.TOWER_COIN_ITEM_ID) >= 500;
        
        // Add goods
        for (int i = 0; i < potentials; i++) {
            // Create potential selector shop item
            var goods = new StarTowerShopGoods(1, 1, 102, 200);
            
            // Add character specific potentials
            if (Utils.generateRandomDouble() < .2) {
                goods.setCharPos(1);
            }
            
            // Add to goods map
            this.addGoods(goods);
        }
        for (int i = 0; i < subNotes; i++) {
            // Randomize sub note
            int id = Utils.randomElement(this.getGame().getSubNoteDropList());
            
            // Create sub note shop item
            StarTowerShopGoods goods = null;
            
            if (hasCoins && Utils.randomChance(.25)) {
                goods = new StarTowerShopGoods(2, 8, id, 400);
            } else {
                goods = new StarTowerShopGoods(2, 3, id, 90);
            }
            
            // Add to goods map
            this.addGoods(goods);
        }
        
        // Apply discounts based on star tower growth nodes
        if (getModifiers().isShopDiscountTier1()) {
            this.applyDiscount(1.0, 2, 0.8);
        }
        if (getModifiers().isShopDiscountTier2()) {
            this.applyDiscount(0.3, 1, 0.5);
        }
        if (getModifiers().isShopDiscountTier3()) {
            this.applyDiscount(1.0, 1, 0.5);
        }
    }
    
    private void applyDiscount(double chance, int times, double percentage) {
        // Check chance
        double random = Utils.generateRandomDouble();
        
        if (random > chance) {
            return;
        }
        
        // Create goods list
        var list = this.getGoods().values().stream()
                .filter(g -> !g.hasDiscount())
                .collect(Collectors.toList());
        
        // Apply discounts
        for (int i = 0; i < times; i++) {
            // Sanity check
            if (list.isEmpty()) {
                break;
            }
            
            // Get goods and apply discount
            var goods = Utils.randomElement(list, true);
            goods.applyDiscount(percentage);
        }
    }
    
    public void addGoods(StarTowerShopGoods goods) {
        this.getGoods().put(getGoods().size() + 1, goods);
    }
    
    @Override
    public StarTowerInteractResp interact(StarTowerInteractReq req, StarTowerInteractResp rsp) {
        // Set nil resp
        rsp.getMutableNilResp();
        
        // Get hawker req
        var hawker = req.getHawkerReq();
        
        if (hawker.hasReRoll()) {
            // Refresh shop items
            this.refresh(rsp);
        } else if (hawker.hasSid()) {
            // Buy shop items
            this.buy(hawker.getSid(), rsp);
        }
        
        // Success
        return rsp;
    }
    
    private void refresh(StarTowerInteractResp rsp) {
        // Check if we can refresh
        if (this.getModifiers().getShopRerollCount() <= 0) {
            return;
        }
        
        // Make sure we have enough currency
        int coin = this.getGame().getResCount(GameConstants.TOWER_COIN_ITEM_ID);
        int price = this.getModifiers().getShopRerollPrice();
        
        if (coin < price) {
            return;
        }
        
        // Create new goods
        this.initGoods();
        
        // Consume reroll count
        this.getGame().getModifiers().consumeShopReroll();
        
        // Set in proto
        rsp.getMutableSelectResp()
            .setHawkerCase(this.toHawkerCaseProto());
        
        // Remove coins
        var change = this.getGame().addItem(GameConstants.TOWER_COIN_ITEM_ID, -price);
        
        // Set change info
        rsp.setChange(change.toProto());
    }
    
    private void buy(int sid, StarTowerInteractResp rsp) {
        // Get goods
        var goods = this.getGoods().get(sid);
        if (goods == null) {
            return;
        }
        
        // Make sure we have enough currency
        int coin = this.getGame().getResCount(GameConstants.TOWER_COIN_ITEM_ID);
        int price = goods.getPrice();
        
        if (coin < price || goods.isSold()) {
            return;
        }
        
        // Mark goods as sold
        goods.markAsSold();
        
        // Create change info
        var change = new PlayerChangeInfo();
        
        // Add goods
        if (goods.getType() == 1) {
            // Potential selector
            int charId = goods.getCharId(this.getGame());
            this.getGame().addCase(rsp.getMutableCases(), this.getGame().createPotentialSelector(charId));
        } else {
            // Sub notes
            this.getGame().addItem(goods.getGoodsId(), goods.getCount(), change);
        }
        
        // Remove coins
        this.getGame().addItem(GameConstants.TOWER_COIN_ITEM_ID, -price, change);
        
        // Set change info
        rsp.setChange(change.toProto());
    }
    
    // Proto
    
    private HawkerCaseData toHawkerCaseProto() {
        var hawker = HawkerCaseData.newInstance();
        
        if (this.getModifiers().getShopRerollCount() > 0) {
            hawker.setCanReRoll(true);
            hawker.setReRollTimes(this.getModifiers().getShopRerollCount());
            hawker.setReRollPrice(this.getModifiers().getShopRerollPrice());
        }
        
        for (var entry : this.getGoods().entrySet()) {
            var sid = entry.getKey();
            var goods = entry.getValue();
            
            var info = HawkerGoods.newInstance()
                    .setSid(sid)
                    .setType(goods.getType())
                    .setIdx(goods.getIdx())
                    .setGoodsId(goods.getGoodsId())
                    .setPrice(goods.getDisplayPrice())
                    .setTag(1);
            
            if (goods.hasDiscount()) {
                info.setDiscount(goods.getPrice());
            }
            
            if (goods.getCharPos() > 0) {
                info.setCharPos(goods.getCharPos());
            }
            
            hawker.addList(info);
        }
        
        return hawker;
    }
    
    @Override
    public void encodeProto(StarTowerRoomCase proto) {
        proto.setHawkerCase(this.toHawkerCaseProto());
    }
}
