package emu.nebula.game.gacha;

import dev.morphia.annotations.Entity;

import emu.nebula.data.resources.GachaDef;
import emu.nebula.data.resources.GachaDef.GachaPackage;
import emu.nebula.data.resources.GachaPkgDef;
import emu.nebula.proto.GachaInformation.GachaInfo;
import emu.nebula.util.Utils;

import lombok.Getter;

@Getter
@Entity(useDiscriminator = false)
public class GachaBannerInfo {
    private int id;
    
    private int total;
    private int missTimesA;
    private int missTimesUpA;
    private int missTimesB;
    private boolean usedGuarantee;
    
    @Deprecated //Morphia only
    public GachaBannerInfo() {
        
    }
    
    public GachaBannerInfo(GachaDef data) {
        this.id = data.getId();
    }
    
    public void setUsedGuarantee(boolean value) {
        this.usedGuarantee = value;
    }
    
    public int doPull(GachaDef data) {
        // Pull chances
        int chanceA = 20;   // 2%
        int chanceB = 100;  // 8%
        
        // 4 star pity
        if (this.missTimesB >= 9) {
            chanceB = 1000;
        }
        
        // 5 star pity
        if (this.missTimesA >= 159) {
            chanceA = 1000;
            chanceB = 0;
        }
        
        // Add miss times
        this.missTimesB++;
        this.missTimesA++;
        //this.missTimesUpA++;
        
        // Get random
        int random = Utils.randomRange(1, 1000);
        GachaPackage gp = null;
        
        if (random <= chanceA) {
            // Reset pity
            this.missTimesA = 0;
            
            // Get A package
            gp = data.getPackageA().next();
        } else if (random <= chanceB) {
            // Add miss times
            this.missTimesB = 0;
            
            // Get B package
            gp = data.getPackageB().next();
        } else {
            // Get C package
            gp = data.getPackageC().next();
        }
        
        // Sanity check
        if (gp == null) {
            return 0;
        }
        
        // Get package
        var pkg = GachaPkgDef.getPackageById(gp.getId());
        if (pkg == null) {
            return 0;
        }
        
        // Add total pulls
        this.total++;
        
        // Get random id
        return pkg.next();
    }
    
    // Proto

    public GachaInfo toProto() {
        var proto = GachaInfo.newInstance()
                .setId(this.getId())
                .setGachaTotalTimes(this.getTotal())
                .setTotalTimes(this.getTotal())
                .setAupMissTimes(this.getMissTimesA())
                .setAMissTimes(this.getMissTimesA())
                .setReveFirstTenReward(true)
                .setRecvGuaranteeReward(this.isUsedGuarantee());
        
        return proto;
    }
}
