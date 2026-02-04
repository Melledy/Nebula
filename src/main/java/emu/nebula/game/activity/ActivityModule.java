package emu.nebula.game.activity;

import emu.nebula.game.GameContext;
import emu.nebula.game.GameContextModule;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import lombok.Getter;

@Getter
public class ActivityModule extends GameContextModule {
    private IntList activities;

    public ActivityModule(GameContext context) {
        super(context);
        this.activities = new IntArrayList();
        
        // Hardcode these activities for now
        // TODO make an activity json file to read activity ids from
        
        // ===== Standard Events =====
        
        // Beyond the dream
        //this.activities.add(1010201);
        //this.activities.add(1010203);
        //this.activities.add(1010204);
        
        // Christmas 2025
        //this.activities.add(2010101);
        //this.activities.add(2010103);
        //this.activities.add(2010104);
        
        // Miracle of a Flicker
        //this.activities.add(1010301);
        //this.activities.add(1010303);
        //this.activities.add(1010304);
        
        // Springseek Festival
        this.activities.add(1010401);
        this.activities.add(1010403);
        this.activities.add(1010404);
        
        // ===== Etc Events =====
        
        // Trial activities
        this.activities.add(700111);
        this.activities.add(700112);

        // Tower defense activity
        this.activities.add(102002);
        
        // Christmas 2025 login events
        this.activities.add(301011);
        this.activities.add(301012);
        
        // Trekker versus
        this.activities.add(600001);
    }
    
}
