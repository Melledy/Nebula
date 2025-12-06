package emu.nebula.game.tower;

import dev.morphia.annotations.Entity;
import lombok.Getter;

@Getter
@Entity(useDiscriminator = false)
public class StarTowerShopGoods {
    private int type;
    private int idx;     // This is actually the shop goods id
    private int goodsId; // Item id
    private int price;
    private int discount;
    private int charPos;
    private boolean sold;
    
    public StarTowerShopGoods(int type, int idx, int goodsId, int price) {
        this.type = type;
        this.idx = idx;
        this.goodsId = goodsId;
        this.price = price;
    }

    public void markAsSold() {
        this.sold = true;
    }
    
    public void setCharPos(int charPos) {
        this.charPos = charPos;
    }
    
    public boolean hasDiscount() {
        return this.getDiscount() > 0;
    }
    
    public void applyDiscount(double percentage) {
        this.discount = (int) Math.ceil(this.price * (1.0 - percentage));
    }
    
    public int getPrice() {
        return this.price - this.discount;
    }
    
    public int getDisplayPrice() {
        return this.price;
    }
    
    public int getCount() {
        if (this.getType() == 2) {
            return this.getIdx() == 8 ? 15 : 5;
        }
        
        return 1;
    }
    
    public int getCharId(StarTowerGame game) {
        if (this.getCharPos() == 0) {
            return 0;
        }
        
        int index = this.getCharPos() - 1;
        return game.getCharIds()[index];
    }
}
