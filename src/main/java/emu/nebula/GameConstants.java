package emu.nebula;

import java.time.ZoneId;

import emu.nebula.game.inventory.ItemParam;
import emu.nebula.util.WeightedList;

public class GameConstants {
    public static final String VERSION = "1.2.0";
    public static int DATA_VERSION = 0;
    
    // Set data versions for each region
    static {
        RegionConfig.getRegion("global")
            .setDataVersion(63);
        
        RegionConfig.getRegion("kr")
            .setDataVersion(70);
        
        RegionConfig.getRegion("jp")
            .setDataVersion(66);
        
        RegionConfig.getRegion("tw")
            .setDataVersion(64);

        RegionConfig.getRegion("cn")
            .setDataVersion(64);
    }
    
    public static final ZoneId UTC_ZONE = ZoneId.of("UTC");
    
    public static final String PROTO_BASE_TYPE_URL = "type.googleapis.com/proto.";
    
    public static final int INTRO_GUIDE_ID = 1;

    public static final int GOLD_ITEM_ID = 1;
    public static final int GEM_ITEM_ID = 2;
    public static final int PREM_GEM_ITEM_ID = 3;
    public static final int ENERGY_BUY_ITEM_ID = GEM_ITEM_ID;
    public static final int EXP_ITEM_ID = 21;
    
    public static final int MAX_ENERGY = 240;
    public static final int ENERGY_REGEN_TIME = 360; // Seconds
    
    public static final int CHARACTER_MAX_GEMS_PER_SLOT = 4;
    public static final int CHARACTER_MAX_GEM_PRESETS = 3;
    public static final int CHARACTER_MAX_GEM_SLOTS = 3;
    
    public static final int MAX_FORMATIONS = 6;
    public static final int MAX_SHOWCASE_IDS = 5;
    
    public static final int BATTLE_PASS_ID = 1;
    
    public static final int MAX_FRIENDSHIPS = 50;
    public static final int MAX_PENDING_FRIENDSHIPS = 30;
    
    public static final int TOWER_COIN_ITEM_ID = 11;
    public static final int[] TOWER_COMMON_SUB_NOTE_SKILLS = new int[] {
        90011, 90012, 90013, 90014, 90015, 90016, 90017
    };
    public static final int[] TOWER_EVENTS_IDS = new int[] {
        101, 102, 104, 105, 106, 107, 108, 114, 115, 116, 126, 127, 128
    };
    
    public static int[][] VAMPIRE_SURVIVOR_BONUS_POWER = new int[][] {
        new int[] {100, 120},
        new int[] {200, 150},
        new int[] {300, 200}
    };
    
    // Daily gifts (Custom)
    
    public static final WeightedList<ItemParam> DAILY_GIFTS = new WeightedList<>();
    
    static {
        DAILY_GIFTS.add(1000, new ItemParam(GOLD_ITEM_ID, 8888));
        DAILY_GIFTS.add(250, new ItemParam(GOLD_ITEM_ID, 18888));
        DAILY_GIFTS.add(250, new ItemParam(33001, 10));
        DAILY_GIFTS.add(10, new ItemParam(GEM_ITEM_ID, 50));
    }
    
    // Helper functions
    
    public static String getGameVersion() {
        // Load data version
        var region = RegionConfig.getRegion(Nebula.getConfig().getRegion());
        
        // Set data version from region
        GameConstants.DATA_VERSION = region.getDataVersion();
        
        // Init game version string
        return VERSION + "." + getDataVersion() + " (" + region.getName().toUpperCase() + ")";
    }
    
    public static int getDataVersion() {
        return Nebula.getConfig().getCustomDataVersion() > 0 ? Nebula.getConfig().getCustomDataVersion() : DATA_VERSION ;
    }
}
