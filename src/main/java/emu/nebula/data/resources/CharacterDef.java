package emu.nebula.data.resources;

import java.util.List;

import emu.nebula.data.BaseDef;
import emu.nebula.data.GameData;
import emu.nebula.data.ResourceType;
import emu.nebula.game.character.ElementType;

import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import lombok.Getter;

@Getter
@ResourceType(name = "Character.json")
public class CharacterDef extends BaseDef {
    private int Id;
    private boolean Visible;
    private boolean Available;
    private String Name;
    private int Grade;
    private int EET;
    
    private int DefaultSkinId;
    private int AdvanceSkinId;
    private int AdvanceSkinUnlockLevel;
    
    private int AdvanceGroup;
    private int[] SkillsUpgradeGroup;

    private int FragmentsId;
    private int TransformQty;
    
    private int[] GemSlots;
    
    // Cached data
    private transient CharacterDesDef des;
    private transient HonorDef honor;
    private transient ElementType elementType;
    private transient List<ChatDef> chats;
    private transient Int2ObjectMap<StarTowerBookPotentialRewardDef> potentialBookRewards;
    
    @Override
    public int getId() {
        return Id;
    }
    
    protected void setDes(CharacterDesDef des) {
        this.des = des;
    }
    
    protected void setHonor(HonorDef honor) {
        this.honor = honor;
    }

    public int getSkillsUpgradeGroup(int index) {
        if (index < 0 || index >= this.SkillsUpgradeGroup.length) {
            return -1;
        }
        
        return this.SkillsUpgradeGroup[index];
    }
    
    public CharGemDef getCharGemData(int slotId) {
        int id = this.GemSlots[slotId - 1];
        return GameData.getCharGemDataTable().get(id);
    }
    
    @Override
    public void onLoad() {
        this.elementType = ElementType.getByValue(this.EET);
        this.chats = new ObjectArrayList<>();
        this.potentialBookRewards = new Int2ObjectLinkedOpenHashMap<>();
    }
}
