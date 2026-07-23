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
        //this.activities.add(1010401);
        //this.activities.add(1010403);
        //this.activities.add(1010404);
        
        // Winter Requiem and the Trigger of Dawn
        //this.activities.add(1010501);
        //this.activities.add(1010503);
        //this.activities.add(1010504);
        
        // To My Dearest You
        //this.activities.add(1010601);
        //this.activities.add(1010603);
        //this.activities.add(1010604);
        
        // Solitary Waltz in Night's Embrace
        //this.activities.add(2010201);
        //this.activities.add(2010203);
        //this.activities.add(2010204);
        
        // Framing the Feelings Unseen
        //this.activities.add(1010701);
        //this.activities.add(1010703);
        //this.activities.add(1010704);
        
        // Heartbeat! Neo-Inspired Rhapsody
        //this.activities.add(1010801);
        //this.activities.add(1010803);
        //this.activities.add(1010804);
        
        // A Sandstorm of Gunfire
        //this.activities.add(1010901);
        //this.activities.add(1010903);
        //this.activities.add(1010904);
        
        // Checking In for A Cozy Summer
        this.activities.add(2010301);
        this.activities.add(2010303);
        this.activities.add(2010304);
        
        // ===== Joint Drills (Finale Echoing) =====
        //this.activities.add(510003); // Causes soft lock in event screen
        
        // ===== Etc Events =====
        
        // Trial activities
        this.activities.add(700123);
        this.activities.add(700124);

        // Tower defense activity
        this.activities.add(102002); // Broken
        
        // Login events
        //this.activities.add(301011);  // Christmas 2025
        //this.activities.add(301012);  // Christmas 2025
        //this.activities.add(301031);  // Stellar Prelude
        //this.activities.add(301033);  // Price of the Star
        
        // Fatebound Stellar Deck
        //this.activities.add(800001); // Causes soft lock in event screen

        // Through Hostile Depths (beta)
        this.activities.add(900001);
        
        // Trekker versus (broken)
        //this.activities.add(600001);
    }
    
}
