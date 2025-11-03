package emu.nebula.game.character;

import org.bson.Document;
import org.bson.types.Binary;
import org.bson.types.ObjectId;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Indexed;
import dev.morphia.annotations.PreLoad;
import emu.nebula.GameConstants;
import emu.nebula.Nebula;
import emu.nebula.data.GameData;
import emu.nebula.data.resources.CharacterDef;
import emu.nebula.data.resources.TalentGroupDef;
import emu.nebula.database.GameDatabaseObject;
import emu.nebula.game.inventory.ItemParamMap;
import emu.nebula.game.player.Player;
import emu.nebula.game.player.PlayerChangeInfo;
import emu.nebula.proto.Public.Char;
import emu.nebula.proto.Public.CharGemPreset;
import emu.nebula.proto.Public.CharGemSlot;
import emu.nebula.proto.PublicStarTower.StarTowerChar;
import emu.nebula.proto.PublicStarTower.StarTowerCharGem;
import emu.nebula.util.Bitset;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import lombok.Getter;

@Getter
@Entity(value = "characters", useDiscriminator = false)
public class Character implements GameDatabaseObject {
    @Id 
    private ObjectId uid;
    @Indexed
    private int playerUid;
    
    private transient CharacterDef data;
    private transient Player player;
    
    private int charId;
    private int advance;
    private int level;
    private int exp;
    private int skin;
    private int[] skills;
    private Bitset talents;
    
    private long createTime;
    
    @Deprecated // Morphia only!
    public Character() {
        
    }
    
    public Character(Player player, int charId) {
        this(player, GameData.getCharacterDataTable().get(charId));
    }
    
    public Character(Player player, CharacterDef data) {
        this.player = player;
        this.playerUid = player.getUid();
        this.charId = data.getId();
        this.data = data;
        this.level = 1;
        this.skin = data.getDefaultSkinId();
        this.skills = new int[] {1, 1, 1, 1, 1};
        this.talents = new Bitset();
        this.createTime = Nebula.getCurrentTime();
    }
    
    public void setPlayer(Player player) {
        this.player = player;
    }

    public void setData(CharacterDef data) {
        if (this.data == null && data.getId() == this.getCharId()) {
            this.data = data;
        }
    }
    
    public int getMaxGainableExp() {
        if (this.getLevel() >= this.getMaxLevel()) {
            return 0;
        }
        
        int maxLevel = this.getMaxLevel();
        int max = 0;
        
        for (int i = this.getLevel() + 1; i <= maxLevel; i++) {
            var data = GameData.getCharacterUpgradeDataTable().get(i);
            
            if (data != null) {
                max += data.getExp();
            }
        }
        
        return Math.max(max - this.getExp(), 0);
    }
    
    public int getMaxExp() {
        if (this.getLevel() >= this.getMaxLevel()) {
            return 0;
        }
        
        var data = GameData.getCharacterUpgradeDataTable().get(this.level + 1);
        return data != null ? data.getExp() : 0;
    }
    
    public int getMaxLevel() {
        return 10 + (this.getAdvance() * 10);
    }
    
    public void addExp(int amount) {
        // Setup
        int expRequired = this.getMaxExp();

        // Add exp
        this.exp += amount;

        // Check for level ups
        while (this.exp >= expRequired && expRequired > 0) {
            this.level += 1;
            this.exp -= expRequired;
            
            expRequired = this.getMaxExp();
        }
        
        // Clamp exp
        if (this.getLevel() >= this.getMaxLevel()) {
            this.exp = 0;
        }
        
        // Save to database
        this.save();
    }
    
    // Handlers
    
    public PlayerChangeInfo upgrade(ItemParamMap params) {
        // Calculate exp gained
        int exp = 0;
        
        // Check if item is an exp item
        for (var entry : params.entries()) {
            var data = GameData.getCharItemExpDataTable().get(entry.getIntKey());
            if (data == null) return null;
            
            exp += data.getExpValue() * entry.getIntValue();
        }
        
        // Clamp exp gain
        exp = Math.min(this.getMaxGainableExp(), exp);
        
        // Calculate gold required
        params.add(GameConstants.GOLD_ITEM_ID, (int) Math.ceil(exp * 0.15D));
        
        // Verify that the player has the items
        if (!this.getPlayer().getInventory().verifyItems(params)) {
            return null;
        }
        
        // Remove items
        var changes = this.getPlayer().getInventory().removeItems(params, null);
        
        // Add exp
        this.addExp(exp);
        
        // Success
        return changes.setSuccess(true);
    }
    
    public PlayerChangeInfo advance() {
        // TODO check player level to make sure they can advance this character
        
        // Get advance data
        int advanceId = (this.getData().getAdvanceGroup() * 100) + (this.advance + 1);
        var data = GameData.getCharacterAdvanceDataTable().get(advanceId);
        
        if (data == null) {
            return null;
        }
        
        // Verify that the player has the items
        if (!this.getPlayer().getInventory().verifyItems(data.getMaterials())) {
            return null;
        }
        
        // Remove items
        var changes = this.getPlayer().getInventory().removeItems(data.getMaterials(), null);
        
        // Add advance level
        this.advance++;
        
        // Save to database
        this.save();
        
        // Success
        return changes.setSuccess(true);
    }
    
    public PlayerChangeInfo upgradeSkill(int index) {
        // TODO check player level to make sure they can advance this character
        
        // Sanity check
        if (index < 0 || index >= this.getSkills().length) {
            return null;
        }
        
        // Get advance data
        int upgradeId = (this.getData().getSkillsUpgradeGroup(index) * 100) + this.getSkills()[index];
        var data = GameData.getCharacterSkillUpgradeDataTable().get(upgradeId);
        
        if (data == null) {
            return null;
        }
        
        // Verify that the player has the items
        if (!this.getPlayer().getInventory().verifyItems(data.getMaterials())) {
            return null;
        }
        
        // Remove items
        var changes = this.getPlayer().getInventory().removeItems(data.getMaterials(), null);
        
        // Add skill level
        this.skills[index]++;
        
        // Save to database
        this.save();
        
        // Success
        return changes.setSuccess(true);
    }
    
    public PlayerChangeInfo unlockTalent(TalentGroupDef talentGroup) {
        // Get talent item
        int talentItemId = this.getData().getFragmentsId();
        int talentItemCount = this.getPlayer().getInventory().getItemCount(talentItemId);
        int unlockCount = (int) Math.floor(talentItemCount / 6); // Max unlock count
        
        // Sanity check
        if (unlockCount <= 0) {
            return null;
        }
        
        // Amount of talents unlocked
        int amount = 0;
        var nodes = new IntArrayList();
        
        // Unlock talents
        for (var talent : talentGroup.getTalents()) {
            // Skip unlocked talents
            if (this.getTalents().isSet(talent.getIndex())) {
                continue;
            }
            
            // Set bit
            this.getTalents().setBit(talent.getIndex());
            
            // Add nodes
            nodes.add(talent.getId());
            amount++;
            
            // Set last talent if we unlocked everything
            if (talent.getSort() == 10) {
                this.getTalents().setBit(talentGroup.getMainTalent().getIndex());
                nodes.add(talentGroup.getMainTalent().getId());
            }
            
            // End
            if (amount >= unlockCount) {
                break;
            }
        }
        
        // Skip if we didn't unlock anything
        if (nodes.size() <= 0) {
            return null;
        }
        
        // Remove items
        var changes = getPlayer().getInventory().removeItem(talentItemId, amount * 6);
        changes.setExtraData(nodes);
        
        // Save to database
        this.save();
        
        // Success
        return changes.setSuccess(true);
    }
    
    public boolean setSkin(int skinId) {
        // Sanity check
        if (this.skin == skinId) {
            return false;
        }
        
        // Make sure we have the skin
        if (!getPlayer().getInventory().hasSkin(skinId)) {
            return false;
        }
        
        // Set skin
        this.skin = skinId;
        
        // Save
        this.save();
        
        // Success
        return true;
    }
    
    // Proto
    
    public Char toProto() {
        var proto = Char.newInstance()
                .setTid(this.getCharId())
                .setLevel(this.getLevel())
                .setSkin(this.getSkin())
                .setAdvance(this.getAdvance())
                .setTalentNodes(this.getTalents().toByteArray())
                .addAllSkillLvs(this.getSkills())
                .setCreateTime(this.getCreateTime());
        
        var gemPresets = proto.getMutableCharGemPresets()
            .getMutableCharGemPresets();
        
        for (int i = 0; i < 3; i++) {
            var preset = CharGemPreset.newInstance()
                    .addAllSlotGem(-1, -1, -1);
            
            gemPresets.add(preset);
        }
        
        for (int i = 1; i <= 3; i++) {
            var slot = CharGemSlot.newInstance()
                    .setId(i);
            
            proto.addCharGemSlots(slot);
        }
        
        proto.getMutableAffinityQuests();
        
        return proto;
    }
    
    public StarTowerChar toStarTowerProto() {
        var proto = StarTowerChar.newInstance()
                .setId(this.getCharId())
                .setAdvance(this.getAdvance())
                .setLevel(this.getLevel())
                .setTalentNodes(this.getTalents().toByteArray())
                .addAllSkillLvs(this.getSkills());
        
        for (int i = 1; i <= 3; i++) {
            var slot = StarTowerCharGem.newInstance()
                    .setSlotId(i)
                    .addAllAttributes(new int[] {0, 0, 0, 0});
            
            proto.addGems(slot);
        }
        
        return proto;
    }
    
    // Database fix
    
    @PreLoad
    public void onLoad(Document doc) {
        var talents = doc.get("talents");
        if (talents != null && talents.getClass() == Binary.class) {
            doc.remove("talents");
            this.talents = new Bitset();
        }
    }
}
