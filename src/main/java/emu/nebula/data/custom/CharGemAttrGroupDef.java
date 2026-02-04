package emu.nebula.data.custom;

import java.util.List;
import java.util.Map;

import emu.nebula.data.BaseDef;
import emu.nebula.data.GameData;
import emu.nebula.data.ResourceType;
import emu.nebula.data.resources.CharGemAttrValueDef;
import emu.nebula.game.character.GameCharacter;
import emu.nebula.util.WeightedList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.Getter;

@Getter
@ResourceType(name = "CharGemAttrGroups.json", useInternal = true)
public class CharGemAttrGroupDef extends BaseDef {
    private int Id;
    private int Weight;
    private IntArrayList AttrTypes;
    private Map<Integer, Integer> UniqueAttrNumWeights;
    
    private transient WeightedList<Integer> uniqueAttrNum;
    private transient List<CharGemAttrTypeData> attributeTypes;
    
    public CharGemAttrGroupDef() {
        this.AttrTypes = new IntArrayList();
    }
    
    @Override
    public int getId() {
        return Id;
    }
    
    public int getRandomUniqueAttrNum() {
        if (this.uniqueAttrNum == null) {
            return 0;
        }
        
        return this.uniqueAttrNum.next();
    }
    
    public CharGemAttrTypeData getRandomAttributeType(GameCharacter character, IntList list) {
        // Setup blacklist to prevent the same attribute from showing up twice
        var blacklist = new IntOpenHashSet();
        
        for (int id : list) {
            var value = GameData.getCharGemAttrValueDataTable().get(id);
            if (value == null) continue;
            
            int blacklistId = value.getTypeId();
            blacklist.add(blacklistId);
        }
        
        // Create random generator
        var random = new WeightedList<CharGemAttrTypeData>();
        
        for (var type : this.getAttributeTypes()) {
            // Don't add types that we already have in the emblem
            if (blacklist.contains(type.getId())) {
                continue;
            }
            
            // Skip attributes that don't match the trekker's element
            if (type.isElementalAttr() && !character.getElementType().getGemAttrTypes().contains(type.getFirstSubType())) {
                continue;
            }
            
            random.add(100, type);
        }
        
        if (random.size() == 0) {
            return null;
        }
        
        return random.next();
    }
    
    @Override
    public void onLoad() {
        // Init unique attribute weights
        this.uniqueAttrNum = new WeightedList<>();
        
        if (this.UniqueAttrNumWeights != null) {
            for (var entry : this.UniqueAttrNumWeights.entrySet()) {
                this.uniqueAttrNum.add(entry.getValue(), entry.getKey());
            }
        }
        
        // Init attribute types
        this.attributeTypes = new ObjectArrayList<>();
        
        for (int id : this.getAttrTypes()) {
            var type = new CharGemAttrTypeData(id);
            this.attributeTypes.add(type);
        }
    }
    
    @Getter
    public static class CharGemAttrTypeData {
        private int id;
        private int firstSubType;
        private WeightedList<CharGemAttrValueDef> values;
        
        public CharGemAttrTypeData(int id) {
            this.id = id;
            this.values = new WeightedList<>();
        }
        
        public boolean isElementalAttr() {
            return this.getFirstSubType() >= 17 && this.getFirstSubType() <= 28;
        }
        
        public void addValue(CharGemAttrValueDef value) {
            this.firstSubType = value.getAttrTypeFirstSubtype();
            this.values.add(value.getRarity(), value);
        }
        
        public CharGemAttrValueDef getRandomValueData() {
            return this.getValues().next();
        }
        
        public int getRandomValue() {
            return this.getRandomValueData().getId();
        }
    }
}
