package emu.nebula.data.resources;

import emu.nebula.data.BaseDef;
import emu.nebula.data.ResourceType;
import emu.nebula.data.ResourceType.LoadPriority;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntCollection;
import lombok.Getter;

@Getter
@ResourceType(name = "GachaATypeProb.json", loadPriority = LoadPriority.HIGH)
public class GachaATypeProbDef extends BaseDef {
    private int Group;
    private int Times;
    private int Prob;

    private static final Int2ObjectMap<Int2IntMap> probByGroupAndTimes = new Int2ObjectOpenHashMap<>();

    @Override
    public int getId() {
        // Compose a key from (Group, Times): high 16 bits = Group, low 16 bits = Times.
        return (Group << 16) | (Times & 0xFFFF);
    }

    @Override
    public void onLoad() {
        // Indexed as group (miss times -> probability) for O(1) runtime lookup
        var groupMap = probByGroupAndTimes.computeIfAbsent(this.Group, i -> new Int2IntOpenHashMap());
        groupMap.put(this.Times, this.Prob);
    }

    public static int getProb(int group, int times, int fallback) {
        var groupMap = probByGroupAndTimes.get(group);
        if (groupMap == null) {
            return fallback;
        }

        if (groupMap.containsKey(times)) {
            return groupMap.get(times);
        }

        // Times=0 is treated as the group's default curve point.
        return groupMap.getOrDefault(0, fallback);
    }

    public static int getMaxProb() {
        int max = 0;
        // Used by the roll engine to scale random range safely when config max exceeds 10000.
        for (var groupMap : probByGroupAndTimes.values()) {
            IntCollection values = groupMap.values();
            for (var valueIterator = values.iterator(); valueIterator.hasNext();) {
                int value = valueIterator.nextInt();
                if (value > max) {
                    max = value;
                }
            }
        }

        return max;
    }
}
