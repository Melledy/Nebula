package emu.nebula.game.activity.type;

import dev.morphia.annotations.Entity;
import emu.nebula.data.resources.ActivityDef;
import emu.nebula.game.activity.ActivityManager;
import emu.nebula.game.activity.GameActivity;
import emu.nebula.game.tower.StarTowerBuild;
import emu.nebula.proto.ActivityDetail.ActivityMsg;
import emu.nebula.proto.Public.TrekkerVersusShow;
import lombok.Getter;
import us.hebi.quickbuf.RepeatedInt;

@Getter
@Entity
public class TrekkerVersusActivity extends GameActivity {
    private int[] charIds;
    private int[] affixIds;
    private int buildScore;
    private int time;
    
    // Apply
    private transient StarTowerBuild build;
    private transient int[] affixList;
    
    @Deprecated // Morphia only
    public TrekkerVersusActivity() {
        
    }
    
    public TrekkerVersusActivity(ActivityManager manager, ActivityDef data) {
        super(manager, data);
    }
    
    public boolean apply(long buildId, RepeatedInt affixes) {
        // Get build
        var build = this.getPlayer().getStarTowerManager().getBuildById(buildId);
        
        if (build == null) {
            return false;
        }
        
        // Set
        this.build = build;
        this.affixList = affixes.toArray();
        
        // Success
        return true;
    }
    
    public void settle(boolean passed, int time) {
        // Sanity check
        if (this.build == null || this.affixList == null) {
            return;
        }
        
        // Check if victory
        if (passed) {
            // Save characters/affixes
            this.charIds = this.build.getCharIds().clone();
            this.affixIds = this.affixList;
            this.time = time;
            this.buildScore = this.build.getScore();
            
            // Save to database
            this.save();
        }
        
        // Clear
        this.build = null;
        this.affixList = null;
    }
    
    // Proto

    @Override
    public void encodeActivityMsg(ActivityMsg msg) {
        msg.getMutableTrekkerVersus()
                .setShow(this.getShowProto());
    }

    public TrekkerVersusShow getShowProto() {
        var show = TrekkerVersusShow.newInstance();
        
        // TODO update
        
        /*
        if (this.getCharIds() != null) {
            show.addAllCharIds(this.getCharIds());
            show.setBuildScore(this.getBuildScore());
        }
        
        if (this.getAffixIds() != null) {
            show.addAllAffixIds(this.getAffixIds());
        }
        */
        
        return show;
    }
}
