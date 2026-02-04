package emu.nebula.game.activity;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.Getter;

public enum ActivityType {
    None          (0),
    PeriodicQuest (1),
    LoginReward   (2),
    Mining        (3),
    Cookie        (4),
    TowerDefense  (5),
    Trial         (6),
    JointDrill    (7),
    CG            (8),
    Levels        (9),
    Avg           (10),
    Task          (11),
    Shop          (12),
    Advertise     (13),
    BDConvert     (14),
    Breakout      (15),
    TrekkerVersus (16),
    Story         (17),
    PenguinCard   (18),
    ThrowGift     (19);

    @Getter
    private final int value;
    private final static Int2ObjectMap<ActivityType> map = new Int2ObjectOpenHashMap<>();

    static {
        for (ActivityType type : ActivityType.values()) {
            map.put(type.getValue(), type);
        }
    }

    private ActivityType(int value) {
        this.value = value;
    }

    public static ActivityType getByValue(int value) {
        return map.get(value);
    }
}
