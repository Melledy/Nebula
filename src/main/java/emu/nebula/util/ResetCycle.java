package emu.nebula.util;

import emu.nebula.GameConstants;

public enum ResetCycle {
    DAILY,
    WEEKLY,
    MONTHLY;

    public static ResetCycle fromRefreshType(int refreshType) {
        return switch (refreshType) {
            case GameConstants.REFRESH_TYPE_DAILY -> DAILY;
            case GameConstants.REFRESH_TYPE_WEEKLY -> WEEKLY;
            case GameConstants.REFRESH_TYPE_MONTHLY -> MONTHLY;
            default -> null;
        };
    }

}
