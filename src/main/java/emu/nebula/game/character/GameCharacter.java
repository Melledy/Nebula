package emu.nebula.game.character;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.bson.types.Binary;
import org.bson.types.ObjectId;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Indexed;
import dev.morphia.annotations.PostLoad;
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
import emu.nebula.game.quest.QuestCondType;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.Notify.Skin;
import emu.nebula.proto.Notify.SkinChange;
import emu.nebula.proto.Public.Char;
import emu.nebula.proto.Public.CharGemPreset;
import emu.nebula.proto.Public.CharGemSlot;
import emu.nebula.proto.Public.UI32;
import emu.nebula.proto.PublicStarTower.StarTowerChar;
import emu.nebula.proto.PublicStarTower.StarTowerCharGem;
import emu.nebula.util.Bitset;
import emu.nebula.util.CustomIntArray;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import lombok.Getter;
import us.hebi.quickbuf.RepeatedInt;

@Getter
@Entity(value = "characters", useDiscriminator = false)
public class GameCharacter implements GameDatabaseObject {
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
    private int affinityLevel;
    private int affinityExp;
    private int skin;
    private int[] skills;
    private Bitset talents;
    private IntSet plots;
    private long createTime;
    
    private int gemPresetIndex;
    private List<CharacterGemPreset> gemPresets;
    private CharacterGemSlot[] gemSlots;
    
    private CharacterContact contact;
    
    @Deprecated // Morphia only!
    public GameCharacter() {
        
    }
    
    public GameCharacter(Player player, int charId) {
        this(player, GameData.getCharacterDataTable().get(charId));
    }
    
    public GameCharacter(Player player, CharacterDef data) {
        this.player = player;
        this.playerUid = player.getUid();
        this.charId = data.getId();
        this.data = data;
        
        this.level = 1;
        this.skin = data.getDefaultSkinId();
        this.skills = new int[] {1, 1, 1, 1, 1};
        this.talents = new Bitset();
        this.gemPresets = new ArrayList<>();
        this.gemSlots = new CharacterGemSlot[GameConstants.CHARACTER_MAX_GEM_SLOTS];
        this.createTime = Nebula.getCurrentTime();
        
        this.contact = new CharacterContact(this);
    }
    
    public void setPlayer(Player player) {
        this.player = player;
    }

    public void setData(CharacterDef data) {
        // Sanity check
        if (this.data != null || data.getId() != this.getCharId()) {
            return;
        }
        
        // Set data
        this.data = data;
        
        // Check contacts
        if (this.contact == null) {
            this.contact = new CharacterContact(this);
            this.save();
        } else {
            this.contact.setCharacter(this);
        }
    }
    
    public void setLevel(int level) {
        this.level = level;
    }
    
    public void setAdvance(int advance) {
        this.advance = advance;
    }

    public void setAffinityLevel(int level) {
        this.affinityLevel = Math.max(level, 0);
        this.affinityExp = 0;
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
        int oldLevel = this.getLevel();

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
        
        // Check if we leveled up
        if (this.level > oldLevel) {
            // Trigger quest
            this.getPlayer().triggerQuest(QuestCondType.CharacterUpTotal, this.level - oldLevel);
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
        if (!this.getPlayer().getInventory().hasItems(params)) {
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
        if (!this.getPlayer().getInventory().hasItems(data.getMaterials())) {
            return null;
        }
        
        // Remove items
        var changes = this.getPlayer().getInventory().removeItems(data.getMaterials(), null);
        
        // Add advance level
        this.advance++;
        
        // Check if we need to add skin
        if (this.getAdvance() == this.getData().getAdvanceSkinUnlockLevel()) {
            // Set advance skin
            this.skin = this.getData().getAdvanceSkinId();
            
            // Add next packages
            this.getPlayer().addNextPackage(
                NetMsgId.character_skin_gain_notify, 
                Skin.newInstance().setNew(UI32.newInstance().setValue(this.getSkin()))
            );
            this.getPlayer().addNextPackage(
                NetMsgId.character_skin_change_notify, 
                SkinChange.newInstance().setCharId(this.getCharId()).setSkinId(this.getSkin())
            );
            
            // Set flag for player to update character skins in their handbook
            this.getPlayer().getCharacters().setUpdateCharHandbook(true);
        }
        
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
        if (!this.getPlayer().getInventory().hasItems(data.getMaterials())) {
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
    
    // Affinity
    
    public int getMaxAffinityExp() {
        var data = GameData.getAffinityLevelDataTable().get(this.affinityLevel + 1);
        return data != null ? data.getNeedExp() : 0;
    }
    
    public void addAffinityExp(int amount) {
        // Setup
        int expRequired = this.getMaxAffinityExp();

        // Add exp
        this.affinityExp += amount;

        // Check for level ups
        while (this.affinityExp >= expRequired && expRequired > 0) {
            this.affinityLevel += 1;
            this.affinityExp -= expRequired;
            
            expRequired = this.getMaxAffinityExp();
        }
        
        // Clamp exp
        if (expRequired <= 0) {
            this.affinityExp = 0;
        }
        
        // Save to database
        this.save();
    }

    public PlayerChangeInfo sendGift(ItemParamMap items) {
        // Verify that the player has the items
        if (!this.getPlayer().getInventory().hasItems(items)) {
            return null;
        }
        
        // Caluclate amount of exp to gain
        var charDescription = this.getData().getDes();
        int exp = 0;
        int count = 0;
        
        for (var item : items) {
            // Get data
            var gift = GameData.getAffinityGiftDataTable().get(item.getIntKey());
            if (gift == null) {
                return null;
            }
            
            // Calculate amount
            double amount = gift.getBaseAffinity() * item.getIntValue();
            
            if (charDescription.isPreferGift(gift)) {
                amount = amount * 1.5D;
            } else if (charDescription.isHateGift(gift)) {
                amount = amount * 0.8D;
            }
            
            // Add
            exp += (int) amount;
            count += item.getIntValue();
        }
        
        // Sanity check
        if (exp <= 0) {
            return null;
        }
        
        // Add affinity exp
        this.addAffinityExp(exp);
        
        // Trigger quest
        this.getPlayer().triggerQuest(QuestCondType.GiftGiveTotal, count);
        
        // Remove items
        var change = this.getPlayer().getInventory().removeItems(items);
        
        // Success
        return change;
    }
    
    public PlayerChangeInfo recvPlotReward(int plotId) {
        // Create change info
        var change = new PlayerChangeInfo();
        
        // Sanity check to prevent players from recving rewards over and over again from the same plot
        if (this.getPlots() != null && this.getPlots().contains(plotId)) {
            return change;
        }
        
        // Get data
        var plot = GameData.getPlotDataTable().get(plotId);
        
        // Sanity check to make sure we can complete this plot quest
        if (plot == null || plot.getChar() != this.getCharId() || plot.getUnlockAffinityLevel() > this.getAffinityLevel()) {
            return change;
        }
        
        // Complete plot
        if (this.plots == null) {
            this.plots = new IntOpenHashSet();
        }
        
        this.getPlots().add(plotId);
        
        // Update to database
        this.save();
        
        // Add items
        this.getPlayer().getInventory().addItems(plot.getRewards(), change);
        
        // Success
        return change;
    }
    
    // Gems
    
    public boolean hasGemPreset(int index) {
        return index >= 0 && index < this.getGemPresets().size();
    }
    
    public CharacterGemPreset getCurrentGemPreset() {
        return this.getGemPreset(this.getGemPresetIndex());
    }
    
    public CharacterGemPreset getGemPreset(int presetIndex) {
        while (this.getGemPresetIndex() >= this.getGemPresets().size()) {
            this.getGemPresets().add(new CharacterGemPreset(this));
        }
        
        return this.getGemPresets().get(presetIndex);
    }
    
    public boolean setCurrentGemPreset(int index) {
        // Sanity check
        if (index < 0 || index >= GameConstants.CHARACTER_MAX_GEM_PRESETS) {
            return false;
        }
        
        // Set current preset and save to database
        this.gemPresetIndex = index;
        this.save();
        
        // Success
        return true;
    }
    
    public boolean renameGemPreset(int index, String name) {
        // Sanity check
        if (index < 0 || index >= GameConstants.CHARACTER_MAX_GEM_PRESETS) {
            return false;
        }
        
        if (name == null || name.length() > 32) {
            return false;
        }
        
        // Rename preset
        var preset = this.getGemPreset(index);
        preset.setName(name);
        
        // Update to database
        this.save();
        
        // Success
        return true;
    }
    
    public CharacterGem getGemFromPreset(CharacterGemPreset preset, int slotId) {
        // Get gem index
        int gemIndex = preset.getGemIndex(slotId - 1);
        
        if (gemIndex <= 0) {
            return null;
        }
        
        // Get gem slot
        var slot = this.getGemSlot(slotId);
        
        if (slot == null) {
            return null;
        }
        
        // Get gem from the slot using preset index
        return slot.getGem(gemIndex);
    }
    
    public CharacterGem getGemFromSlot(int slotId, int gemIndex) {
        // Check if gem slot exists
        if (!this.hasGemSlot(slotId)) {
            return null;
        }
        
        // Get gem from gem slot
        var slot = this.getGemSlot(slotId);
        var gem = slot.getGem(gemIndex);
        
        return gem;
    }
    
    public boolean equipGem(int presetIndex, int slotId, int gemIndex) {
        // Sanity check
        if (presetIndex < 0 || presetIndex >= GameConstants.CHARACTER_MAX_GEM_PRESETS) {
            return false;
        }
        
        // Get preset
        var preset = this.getGemPreset(presetIndex);
        
        // Set gem index in preset
        boolean success = preset.setGemIndex(slotId, gemIndex);
        
        // Save if successful
        if (success) {
            this.save();
        }
        
        return success;
    }
    
    public boolean hasGemSlot(int slotId) {
        // Calculate index from slot id
        int index = slotId - 1;
        
        // Sanity check
        if (index < 0 || index >= this.getGemSlots().length) {
            return false;
        }
        
        return this.gemSlots[index] != null;
    }
    
    public CharacterGemSlot getGemSlot(int slotId) {
        // Calculate index from slot id
        int index = slotId - 1;
        
        // Sanity check
        if (index < 0 || index >= this.getGemSlots().length) {
            return null;
        }
        
        // Create gem slot object if it doesnt exist
        if (this.gemSlots[index] == null) {
            this.gemSlots[index] = new CharacterGemSlot(slotId);
        }
        
        return this.gemSlots[index];
    }
    
    public boolean lockGem(int slotId, int gemIndex, boolean lock) {
        // Get gem from slot
        var gem = this.getGemFromSlot(slotId, gemIndex);
        if (gem == null) return false;
        
        // Lock
        gem.setLocked(lock);
        
        // Save to database
        this.save();
        
        // Success
        return true;
    }

    public synchronized PlayerChangeInfo generateGem(int slotId) {
        // Get gem slot
        var slot = this.getGemSlot(slotId);
        if (slot == null) {
            return null;
        }
        
        // Skip if slot is full
        if (slot.isFull()) {
            return null;
        }
        
        // Get gem data
        var gemData = this.getData().getCharGemData(slotId);
        var gemControl = gemData.getControlData();
        
        // Check character level
        if (this.getLevel() < gemControl.getUnlockLevel()) {
            return null;
        }
        
        // Make sure the player has the materials to craft the emblem
        if (!getPlayer().getInventory().hasItem(gemData.getGenerateCostTid(), gemControl.getGeneratenCostQty())) {
            return null;
        }
        
        // Generate attributes and create gem
        var attributes = gemControl.generateAttributes();
        var gem = new CharacterGem(attributes);
        
        // Add gem to slot
        slot.getGems().add(gem);
        
        // Save to database
        this.save();
        
        // Consume materials
        var change = getPlayer().getInventory().removeItem(gemData.getGenerateCostTid(), gemControl.getGeneratenCostQty());
        
        // Set change info extra info
        change.setExtraData(gem);
        
        // Success
        return change;
    }
    
    @SuppressWarnings("deprecation")
    public synchronized PlayerChangeInfo refreshGem(int slotId, int gemIndex, RepeatedInt lockedAttributes) {
        // Get gem from slot
        var gem = this.getGemFromSlot(slotId, gemIndex);
        if (gem == null) return null;
        
        // Get gem data
        var gemData = this.getData().getCharGemData(slotId);
        var gemControl = gemData.getControlData();
        
        // Check character level
        if (this.getLevel() < gemControl.getUnlockLevel()) {
            return null;
        }
        
        // Get locked attributes
        if (lockedAttributes.length() > gemControl.getLockableNum()) {
            return null;
        }
        
        // Calculate the materials we need
        var materials = new ItemParamMap();
        materials.add(gemData.getRefreshCostTid(), gemControl.getRefreshCostQty());
        materials.add(gemControl.getLockItemTid(), gemControl.getLockItemQty() * lockedAttributes.length());
        
        // Make sure the player has the materials to craft the emblem
        if (!getPlayer().getInventory().hasItems(materials)) {
            return null;
        }
        
        // Create base list of attributes
        var list = new CustomIntArray();
        
        // Add locked attributes to list
        if (lockedAttributes.length() != 0) {
            var locked = new IntOpenHashSet();
            lockedAttributes.forEach(locked::add);
            
            for (int i = 0; i < gem.getAttributes().length; i++) {
                int attr = gem.getAttributes()[i];
                
                if (locked.contains(attr)) {
                    list.add(i, attr);
                }
            }
        }
        
        // Generate attributes and create gem
        var attributes = gemControl.generateAttributes(list);
        gem.setNewAttributes(attributes);
        
        // Save to database
        this.save();
        
        // Consume materials
        var change = getPlayer().getInventory().removeItems(materials);
        
        // Set change info extra info
        change.setExtraData(gem);
        
        // Success
        return change;
    }
    
    public boolean replaceGemAttributes(int slotId, int gemIndex) {
        // Get gem from slot
        var gem = this.getGemFromSlot(slotId, gemIndex);
        if (gem == null) return false;
        
        // Replace attributes with altered ones
        boolean success = gem.replaceAttributes();
        
        // Save to database
        if (success) {
            this.save();
        }
        
        // Success
        return success;
    }
    
    // Proto
    
    public Char toProto() {
        var proto = Char.newInstance()
                .setTid(this.getCharId())
                .setLevel(this.getLevel())
                .setSkin(this.getSkin())
                .setAdvance(this.getAdvance())
                .setAffinityLevel(this.getAffinityLevel())
                .setAffinityExp(this.getAffinityExp())
                .setTalentNodes(this.getTalents().toByteArray())
                .addAllSkillLvs(this.getSkills())
                .setCreateTime(this.getCreateTime());

        // Encode gem presets
        var gemPresets = proto.getMutableCharGemPresets()
                .setInUsePresetIndex(this.getGemPresetIndex())
                .getMutableCharGemPresets();
        
        for (int i = 0; i < GameConstants.CHARACTER_MAX_GEM_PRESETS; i++) {
            CharGemPreset info = null;
                    
            if (this.hasGemPreset(i)) {
                info = getGemPresets().get(i).toProto();
            } else {
                info = CharGemPreset.newInstance()
                        .addAllSlotGem(-1, -1, -1);
            }
            
            gemPresets.add(info);
        }
        
        // Encode gems
        for (int i = 1; i <= GameConstants.CHARACTER_MAX_GEM_SLOTS; i++) {
            if (this.hasGemSlot(i)) {
                var slot = this.getGemSlot(i);
                proto.addCharGemSlots(slot.toProto());
            } else {
                proto.addCharGemSlots(CharGemSlot.newInstance().setId(i));
            }
        }
        
        // Affinity quests
        proto.getMutableAffinityQuests();
        
        // Encode plots
        if (this.getPlots() != null) {
            this.getPlots().forEach(proto::addPlots);
        }
        
        // Finish
        return proto;
    }
    
    public StarTowerChar toStarTowerProto() {
        var proto = StarTowerChar.newInstance()
                .setId(this.getCharId())
                .setAdvance(this.getAdvance())
                .setLevel(this.getLevel())
                .setAffinityLevel(this.getAffinityLevel())
                .setTalentNodes(this.getTalents().toByteArray())
                .addAllSkillLvs(this.getSkills());
        
        // Encode gems
        var preset = this.getCurrentGemPreset();
        
        for (int i = 1; i <= preset.getLength(); i++) {
            var gem = this.getGemFromPreset(preset, i);
            var info = StarTowerCharGem.newInstance()
                    .setSlotId(i);
            
            if (gem != null) {
                info.addAllAttributes(gem.getAttributes());
            } else {
                info.addAllAttributes(new int[] {0, 0, 0, 0});
            }
            
            proto.addGems(info);
        }
        
        return proto;
    }
    
    // Database fix
    
    @PreLoad
    public void preLoad(Document doc) {
        var talents = doc.get("talents");
        if (talents != null && talents.getClass() == Binary.class) {
            doc.remove("talents");
            this.talents = new Bitset();
        }
    }
    
    @PostLoad
    public void postLoad() {
        if (this.gemSlots == null) {
            // Create gem slots array if it didn't exist
            this.gemSlots = new CharacterGemSlot[GameConstants.CHARACTER_MAX_GEM_SLOTS];
        }
        
        if (this.gemPresets == null) {
            // Create gem presets list if it didn't exist
            this.gemPresets = new ArrayList<>();
        }
    }
}
