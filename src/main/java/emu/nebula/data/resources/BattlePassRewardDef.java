package emu.nebula.data.resources;

import emu.nebula.data.BaseDef;
import emu.nebula.data.ResourceType;
import emu.nebula.game.inventory.ItemParamMap;
import lombok.Getter;

@Getter
@ResourceType(name = "BattlePassReward.json")
public class BattlePassRewardDef extends BaseDef {
    private int ID;
    private int Level;
    
    private int Tid1;
    private int Qty1;
    private int Tid2;
    private int Qty2;
    private int Tid3;
    private int Qty3;
    private boolean Focus;
    
    private transient ItemParamMap basicRewards;
    private transient ItemParamMap premiumRewards;
    private transient ItemParamMap luxuryRewards;
    
    @Override
    public int getId() {
        return (ID << 16) + Level;
    }
    
    @Override
    public void onLoad() {
        this.basicRewards = new ItemParamMap();
        this.premiumRewards = new ItemParamMap();
        this.luxuryRewards = new ItemParamMap();
        
        // Basic rewards (Tid1) - for all players
        if (this.Tid1 > 0) {
            this.basicRewards.add(this.Tid1, this.Qty1);
        }
        
        // Premium rewards (Tid2) - for both 58 and 98 yuan tiers
        if (this.Tid2 > 0) {
            this.premiumRewards.add(this.Tid2, this.Qty2);
        }
        
        // Luxury rewards (Tid3) - ONLY for 98 yuan tier
        if (this.Tid3 > 0) {
            this.luxuryRewards.add(this.Tid3, this.Qty3);
        }
    }
    
    public boolean hasLuxuryRewards() {
        return this.luxuryRewards != null && !this.luxuryRewards.isEmpty();
    }

}
