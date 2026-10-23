package emu.nebula;

import java.time.ZoneId;

import emu.nebula.game.inventory.ItemParam;
import emu.nebula.game.inventory.ItemParamMap;
import emu.nebula.util.WeightedList;

public class GameConstants {
    public static final String VERSION = "2.0.0";
    public static int DATA_VERSION = 0;
    
    public static final ZoneId UTC_ZONE = ZoneId.of("UTC");
    
    public static final String PROTO_BASE_TYPE_URL = "type.googleapis.com/proto.";
    
    public static final int INTRO_GUIDE_ID = 1;
    public static final int DEFAULT_HONOR_ID = 111001;

    // Item ids
    public static final int GOLD_ITEM_ID = 1;
    public static final int STELLANITE_DUST_ITEM_ID = 2;
    public static final int PAID_STELLANITE_LUMINA_ITEM_ID = 3;
    public static final int FREE_STELLANITE_LUMINA_ITEM_ID = 4;
    public static final int ENERGY_BUY_ITEM_ID = STELLANITE_DUST_ITEM_ID;
    public static final int EXP_ITEM_ID = 21;
    public static final int WEEKLY_ENTRY_ITEM_ID = 28;
    public static final int JOINT_DRILL_TICKET_ID = 36;
    public static final int DORM_FURNITURE_COIN_ID = 701;
    
    // Energy
    public static final int MAX_ENERGY = 240;
    public static final int ENERGY_REGEN_TIME = 360; // Timeunit = Seconds
    
    // Characters
    public static final int CHARACTER_MAX_GEMS_PER_SLOT = 4;
    public static final int CHARACTER_MAX_GEM_PRESETS = 3;
    public static final int CHARACTER_MAX_GEM_SLOTS = 3;
    public static final int CHARACTER_MAX_OVERLOCK_COUNT = 2;
    
    public static final int CHARACTER_TAG_VANGUARD = 101;
    public static final int CHARACTER_TAG_VERSATILE = 102;
    public static final int CHARACTER_TAG_SUPPORT = 103;
    
    public static final int AFFINITY_HONOR_UNLOCK_LEVEL = 10;
    
    // Client limits
    public static final int MAX_FORMATIONS = 6;
    public static final int MAX_SHOWCASE_IDS = 5;
    public static final int MAX_BUILDS = 100;
    public static final int MAX_PRESETS = 50;
    public static final int MAX_VITA_PROJECTS = 5;

    public static final int MAX_GACHA_HISTORY_SAVE_RETRIES = 2;
    
    public static final int MAX_FRIENDSHIPS = 50;
    public static final int MAX_PENDING_FRIENDSHIPS = 30;
    
    // Battle Pass
    public static final int BATTLE_PASS_ID = 1; // Default battle pass
    public static final int BATTLE_PASS_UNLOCK_LEVEL = 3;
    
    // Star Tower (Monolith)
    public static final int TOWER_COIN_ITEM_ID = 11;
    public static final int[] TOWER_EVENTS_IDS = new int[] {
        101, 102, 104, 105, 106, 107, 108, 114, 115, 116, 126, 127, 128
    };

    // Star Tower Vita (auto decision)
    public static final int VITA_ENERGY_ITEM_ID = 42;
    public static final int VITA_ENERGY_MAX = 10_000;
    public static final int VITA_ENERGY_PURCHASE_CURRENCY_ID = STELLANITE_DUST_ITEM_ID;
    public static final int VITA_ENERGY_PURCHASE_PRICE = 10;
    public static final int VITA_ENERGY_PURCHASE_VALUE = 3_000;
    public static final int VITA_ENERGY_COST = 15;
    
    // Trace Hunt
    public static final int TRACE_HUNT_TOKEN_ID = 37;
    public static final int TRACE_HUNT_REQUEST_ITEM_ID = 38;
    public static final int TRACE_HUNT_PERMIT_ITEM_ID = 39;
    public static final int TRACE_HUNT_MAX_DAILY_REQUESTS = 20;
    public static final int TRACE_HUNT_MAX_DAILY_PERMITS = 4;
    public static final int[] TRACE_HUNT_TRACE_IDS = {21, 22, 23, 31, 41};
    public static final int TRACE_HUNT_MAX_TRACE_PROGRESS = 10_000;
    public static final int TRACE_HUNT_MAX_HUNT_PROGRESS = 10_000;

    // Dorm
    public static final int DORM_MAX_PRESET_NUM = 20;
    public static final int DORM_DEFAULT_PRESET_NUM = 10;
    public static final int DORM_MAX_ROOM_NUM = 4;
    public static final int DORM_INVITATION_ROLE_NORMAL_NUM = 3;
    public static final int DORM_INVITATION_ROLE_RAND_NUM = 1;
    public static final int DORM_ROLE_INTERACTION_MAX_NUM = 5;
    public static final int DORM_ROLE_INTERACTION_DROP_ID = 4002;
    public static final int DORM_ROLE_FAVORABILITY = 300;
    public static final int DORM_ROLE_AFFINITY_GIFT_MAX_NUM = 5;
    public static final int DORM_ROLE_AFFINITY_GIFT_DAILY_NUM = 1;
    public static final int DORM_NAME_RESET_TIME_LIMIT = 604_800;
    public static final int DORM_LIKE_LIMIT = 3;
    public static final int DORM_BLOCKS_CONVERT_PROP = 1;
    public static final int DORM_DEFAULT_ROOM_ID = 701001;
    public static final int DORM_MAX_NAME_LENGTH = 20;
    
    // Etc
    public static final int REFRESH_TYPE_DAILY = 1;
    public static final int REFRESH_TYPE_WEEKLY = 2;
    public static final int REFRESH_TYPE_MONTHLY = 3;

    public static final int CURRENCY_TYPE_CASH = 1;
    public static final int CURRENCY_TYPE_ITEM = 2; // Stellanite Lumina
    public static final int CURRENCY_TYPE_FREE = 3;
    public static final int TAG_SKIN = 2;
    
    public static int[][] VAMPIRE_SURVIVOR_BONUS_POWER = new int[][] {
        new int[] {100, 120},
        new int[] {200, 150},
        new int[] {300, 200}
    };

    public static final int UNLIMITED_STOCK = Integer.MAX_VALUE;
    
    // Daily gifts (Custom)
    public static final WeightedList<ItemParam> DAILY_SHOP_GIFTS = new WeightedList<>();
    public static final WeightedList<ItemParamMap> DAILY_MALL_GIFTS = new WeightedList<>();
    
    static {
        DAILY_SHOP_GIFTS.add(1000, new ItemParam(GOLD_ITEM_ID, 8888));
        DAILY_SHOP_GIFTS.add(250, new ItemParam(GOLD_ITEM_ID, 18888));
        DAILY_SHOP_GIFTS.add(100, new ItemParam(GOLD_ITEM_ID, 28888));
        DAILY_SHOP_GIFTS.add(250, new ItemParam(33001, 10));
        DAILY_SHOP_GIFTS.add(10, new ItemParam(STELLANITE_DUST_ITEM_ID, 50)); // Custom
        
        DAILY_MALL_GIFTS.add(100, ItemParamMap.of(GOLD_ITEM_ID, 25_000, 82007, 5));
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
