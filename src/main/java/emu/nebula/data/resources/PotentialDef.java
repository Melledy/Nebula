package emu.nebula.data.resources;

import emu.nebula.data.BaseDef;
import emu.nebula.data.ResourceType;
import emu.nebula.game.tower.StarTowerGame;
import lombok.Getter;

@Getter
@ResourceType(name = "Potential.json")
public class PotentialDef extends BaseDef {
    private int Id;
    private int CharId;
    private int Build;
    private int BranchType;
    private int MaxLevel;
    private int[] BuildScore;
    
    private String BriefDesc;
    
    @Override
    public int getId() {
        return Id;
    }
    
    public int getMaxLevel() {
        // Check if regular potential
        if (this.BranchType == 3) {
            return this.BuildScore.length;
        }
        
        // Special potential should always have a max level of 1
        return this.MaxLevel;
    }
    
    public int getMaxLevel(StarTowerGame game) {
        // Check if regular potential
        if (this.BranchType == 3) {
            return this.MaxLevel + game.getModifiers().getBonusMaxPotentialLevel();
        }
        
        // Special potential should always have a max level of 1
        return this.MaxLevel;
    }

    public int getBuildScore(int level) {
        int index = level - 1;
        
        if (index >= this.BuildScore.length) {
            index = this.BuildScore.length - 1;
        }
        
        return this.BuildScore[index];
    }
}
