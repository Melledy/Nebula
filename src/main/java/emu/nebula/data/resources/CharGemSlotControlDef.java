package emu.nebula.data.resources;

import emu.nebula.data.BaseDef;
import emu.nebula.data.GameData;
import emu.nebula.data.ResourceType;
import emu.nebula.data.custom.CharGemAttrGroupDef;
import emu.nebula.game.character.GameCharacter;
import emu.nebula.util.Utils;
import emu.nebula.util.WeightedList;
import emu.nebula.util.ints.CustomIntArray;
import it.unimi.dsi.fastutil.ints.IntList;
import lombok.Getter;

@Getter
@ResourceType(name = "CharGemSlotControl.json")
public class CharGemSlotControlDef extends BaseDef {
    private int Id;
    private int Position;
    private int MaxAlterNum;
    private int UnlockLevel;
    
    private int GeneratenCostQty;
    private int RefreshCostQty;
    
    private int LockableNum;
    private int LockItemTid;
    private int LockItemQty;
    
    // These have to be hardcoded now
    private transient int UniqueAttrGroupProb;
    private transient int UniqueAttrGroupId;
    private transient int[] AttrGroupId;
    
    @Override
    public int getId() {
        return Id;
    }
    
    public IntList generateAttributes(GameCharacter character) {
        return this.generateAttributes(character, new CustomIntArray());
    }
    
    // Check if we should add unique attributes based on the probablity
    private boolean shouldAddUniqueAttr() {
        int random = Utils.randomRange(1, 10000);
        return random <= this.UniqueAttrGroupProb;
    }
    
    public IntList generateAttributes(GameCharacter character, CustomIntArray list) {
        // Add unique attributes
        if (this.UniqueAttrGroupId > 0 && this.shouldAddUniqueAttr()) {
            var group = GameData.getCharGemAttrGroupDataTable().get(this.UniqueAttrGroupId);
            int num = group.getRandomUniqueAttrNum();
            
            for (int i = 0; i < num; i++) {
                var attributeType = group.getRandomAttributeType(character, list);
                if (attributeType == null) {
                    break;
                }
                
                list.add(attributeType.getRandomValue());
            }
            
            if (list.getValueCount() >= this.MaxAlterNum) {
                return list;
            }
        }
        
        // Get random attributes
        var random = new WeightedList<CharGemAttrGroupDef>();
        
        for (var groupId : this.AttrGroupId) {
            var group = GameData.getCharGemAttrGroupDataTable().get(groupId);
            if (group == null || group.getWeight() == 0) {
                continue;
            }
            
            random.add(group.getWeight(), group);
        }
        
        // Add up to 4 attributes
        while (list.getValueCount() < this.MaxAlterNum) {
            var group = random.next();
            var attributeType = group.getRandomAttributeType(character, list);
            
            list.add(attributeType.getRandomValue());
        }
        
        // Complete
        return list;
    }
    
    @Override
    public void onLoad() {
        // Hard coded values
        switch (this.Id) {
            case 3:
                this.UniqueAttrGroupProb = 3300;
                this.UniqueAttrGroupId = 12;
                this.AttrGroupId = new int[] {9, 10};
                break;
            case 2:
                this.UniqueAttrGroupProb = 4000;
                this.UniqueAttrGroupId = 11;
                this.AttrGroupId = new int[] {5, 6, 7, 8};
                break;
            case 1:
            default:
                this.AttrGroupId = new int[] {1, 2, 3, 4};
                break;
        }
    }
}
