package emu.nebula.data.resources;

import emu.nebula.data.BaseDef;
import emu.nebula.data.ResourceType;
import emu.nebula.util.JsonUtils;
import it.unimi.dsi.fastutil.ints.IntSet;
import lombok.Getter;

@Getter
@ResourceType(name = "DormReward.json")
public class DormRewardDef extends BaseDef {
    private int Id;
    private int DropId;
    private String Conditions;

    private transient int[][] parsedConditions;

    @Override
    public int getId() {
        return Id;
    }

    @Override
    public void onLoad() {
        this.parsedConditions = JsonUtils.decode(this.Conditions, int[][].class);
    }

    /**
     * Checks the requirements for this dorm reward.
     * Condition type 1 = the player needs to have the room unlocked.
     */
    public boolean isUnlocked(IntSet unlockedRoomIds) {
        if (this.parsedConditions == null) {
            return false;
        }

        for (int[] condition : this.parsedConditions) {
            if (condition.length < 2) {
                return false;
            }

            switch (condition[0]) {
                case 1 -> {
                    if (!unlockedRoomIds.contains(condition[1])) {
                        return false;
                    }
                }
                default -> {
                    return false;
                }
            }
        }

        return true;
    }
}
