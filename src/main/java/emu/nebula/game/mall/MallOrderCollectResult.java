package emu.nebula.game.mall;

import emu.nebula.proto.Public.ChangeInfo;
import lombok.Getter;

/**
 * Separate real reward delta from popup display data
 * Decouple settlement logic from client UI, prevent data mutation on reuse
 */
@Getter
public final class MallOrderCollectResult {
    private final ChangeInfo stateChange;
    private final ChangeInfo displayChange;

    private MallOrderCollectResult(ChangeInfo stateChange, ChangeInfo displayChange) {
        this.stateChange = copyOf(stateChange);
        this.displayChange = copyOf(displayChange);
    }

    public static MallOrderCollectResult empty() {
        return new MallOrderCollectResult(null, null);
    }

    public static MallOrderCollectResult of(ChangeInfo change) {
        return new MallOrderCollectResult(change, change);
    }

    public static MallOrderCollectResult split(ChangeInfo stateChange, ChangeInfo displayChange) {
        return new MallOrderCollectResult(stateChange, displayChange);
    }

    public boolean hasStateChange() {
        return !stateChange.isEmpty();
    }

    public boolean hasDisplayChange() {
        return !displayChange.isEmpty();
    }

    private static ChangeInfo copyOf(ChangeInfo change) {
        if (change == null || change.isEmpty()) {
            return ChangeInfo.newInstance();
        }

        return ChangeInfo.newInstance().copyFrom(change);
    }

}
