package emu.nebula;

import java.time.ZoneId;

public class GameConstants {
    public static final int DATA_VERSION = 40;
    public static final String VERSION = "1.0.0." + DATA_VERSION;
    
    public static final ZoneId UTC_ZONE = ZoneId.of("UTC");
    
    public static final String PROTO_BASE_TYPE_URL = "type.googleapis.com/proto.";
    
    public static final int INTRO_GUIDE_ID = 1;

    public static final int GOLD_ITEM_ID = 1;
    public static final int GEM_ITEM_ID = 2;
    public static final int PREM_GEM_ITEM_ID = 3;
    public static final int ENERGY_BUY_ITEM_ID = GEM_ITEM_ID;
    public static final int STAR_TOWER_GOLD_ITEM_ID = 11;
    public static final int EXP_ITEM_ID = 21;
    
    public static final int MAX_ENERGY = 240;
    public static final int ENERGY_REGEN_TIME = 360; // Seconds
    
    public static final int MAX_FORMATIONS = 5;
    public static final int MAX_SHOWCASE_IDS = 5;
    
    public static final int BATTLE_PASS_ID = 1;
    
    public static final int MAX_FRIENDSHIPS = 50;
    public static final int MAX_PENDING_FRIENDSHIPS = 30;
}
