package emu.nebula.game.character;

import java.util.Arrays;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import lombok.Getter;

@Getter
public enum ElementType {
    INHERIT     (0),
    AQUA        (1, 90018, new int[] {17, 23}),
    FIRE        (2, 90019, new int[] {18, 24}),
    EARTH       (3, 90021, new int[] {19, 25}),
    WIND        (4, 90020, new int[] {20, 26}),
    LIGHT       (5, 90022, new int[] {21, 27}),
    DARK        (6, 90023, new int[] {22, 28}),
    NONE        (7);
    
    private final int value;
    private final int subNoteSkillItemId;
    private final IntSet gemAttrTypes;
    
    private final static Int2ObjectMap<ElementType> map = new Int2ObjectOpenHashMap<>();
    
    static {
        for (ElementType type : ElementType.values()) {
            map.put(type.getValue(), type);
        }
    }
    
    private ElementType(int value) {
        this(value, 0, new int[0]);
    }
    
    private ElementType(int value, int subNoteSkillItemId, int[] attrTypes) {
        this.value = value;
        this.subNoteSkillItemId = subNoteSkillItemId;
        
        this.gemAttrTypes = new IntOpenHashSet();
        Arrays.stream(attrTypes).forEach(this.gemAttrTypes::add);
    }
    
    public static ElementType getByValue(int value) {
        return map.get(value);
    }
}
