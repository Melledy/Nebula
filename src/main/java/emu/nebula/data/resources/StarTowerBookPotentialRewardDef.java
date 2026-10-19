package emu.nebula.data.resources;

import java.util.Arrays;

import emu.nebula.data.BaseDef;
import emu.nebula.data.GameData;
import emu.nebula.data.ResourceType;
import emu.nebula.data.ResourceType.LoadPriority;

import lombok.Getter;

@Getter
@ResourceType(name = "StarTowerBookPotentialReward.json", loadPriority = LoadPriority.LOW)
public class StarTowerBookPotentialRewardDef extends BaseDef {
    private int Id;
    private int CharId;
    private int Sort;
    private String Params;
    private int ItemId;
    private int ItemQty;
    
    // Cached data
    private transient int[] conditions;
    
    @Override
    public int getId() {
        return Id;
    }
    
    public int getRequiredPotentials() {
        if (this.conditions == null || this.conditions.length < 2) {
            return Integer.MAX_VALUE;
        }
        
        return this.conditions[1];
    }
    
    @Override
    public void onLoad() {
        this.conditions = Arrays.stream(Params.replaceAll("[\\[\\] ]", "")
                .split(","))
                .mapToInt(Integer::parseInt)
                .toArray();
        
        var characterDef = GameData.getCharacterDataTable().get(this.CharId);
        if (characterDef != null) {
            characterDef.getPotentialBookRewards().put(this.getSort(), this);
        }
    }
}
