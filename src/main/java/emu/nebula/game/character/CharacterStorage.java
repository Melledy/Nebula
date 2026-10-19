package emu.nebula.game.character;

import java.util.ArrayList;
import java.util.Collection;

import emu.nebula.Nebula;
import emu.nebula.data.GameData;
import emu.nebula.data.resources.CharacterDef;
import emu.nebula.data.resources.DiscDef;
import emu.nebula.game.player.PlayerManager;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.Public.HandbookInfo;
import emu.nebula.util.Bitset;
import emu.nebula.game.achievement.AchievementCondition;
import emu.nebula.game.inventory.ItemParamMap;
import emu.nebula.game.player.Player;
import emu.nebula.game.player.PlayerChangeInfo;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.Getter;
import lombok.Setter;

@Getter
public class CharacterStorage extends PlayerManager {
    private final Int2ObjectMap<GameCharacter> characters;
    private final Int2ObjectMap<GameDisc> discs;
    
    // Flags
    @Setter private boolean hasAddedChar;
    @Setter private boolean hasAddedDisc;
    
    @Setter private boolean updateCharHandbook;
    @Setter private boolean updateDiscHandbook;
    
    public CharacterStorage(Player player) {
        super(player);
        
        this.characters = new Int2ObjectOpenHashMap<>();
        this.discs = new Int2ObjectOpenHashMap<>();
    }
    
    // Characters

    public GameCharacter getCharacterById(int id) {
        if (id <= 0) {
            return null;
        }
        
        return this.characters.get(id);
    }
    
    public GameCharacter getCharacterFromPotential(int potentialId) {
        int charId = (potentialId / 100) % 1000;
        return this.getCharacterById(charId);
    }
    
    public boolean hasCharacter(int id) {
        return this.characters.containsKey(id);
    }
    
    public GameCharacter addCharacter(int charId) {
        // Sanity check to make sure we dont have this character already
        if (this.hasCharacter(charId)) {
            return null;
        }
        
        // Get data
        var data = GameData.getCharacterDataTable().get(charId);
        if (data == null) return null;
        
        // Add character
        return this.addCharacter(data);
    }

    public GameCharacter addCharacter(CharacterDef data) {
        // Sanity check to make sure we dont have this character already
        if (this.hasCharacter(data.getId())) {
            return null;
        }
        
        // Prevent players from getting unavliable characters
        if (!data.isAvailable()) {
            return null;
        }
        
        // Create character
        var character = new GameCharacter(this.getPlayer(), data);
        
        // Save to database
        character.save();
        
        // Add to characters
        this.characters.put(character.getCharId(), character);
        
        // Set flags for player to update character skins in their handbook
        this.setUpdateCharHandbook(true);
        this.setHasAddedChar(true);
        
        // Return character
        return character;
    }
    
    public Collection<GameCharacter> getCharacterCollection() {
        return this.getCharacters().values();
    }
    
    public HandbookInfo getCharacterHandbook() {
        var bitset = new Bitset();
        
        for (var skinId : getPlayer().getInventory().getAllSkinIds()) {
            // Get handbook data
            var data = GameData.getHandbookDataTable().get(400000 + skinId);
            if (data == null) continue;
            
            // Set flag
            bitset.setBit(data.getIndex());
        }
        
        var handbook = HandbookInfo.newInstance()
                .setType(1)
                .setData(bitset.toByteArray());
        
        return handbook;
    }
    
    public void triggerCharacterAchievements(GameCharacter character) {
        // Create elements we want to trigger the achievement for
        var elements = new ItemParamMap();
        elements.put(character.getData().getElementType().getValue(), 0);

        // Calculate the amount of characters with the elements
        this.triggerCharacterAchievements(elements, character.getLevel());
    }
    
    public void triggerCharacterAchievements(Collection<GameCharacter> characters, int level) {
        var elements = new ItemParamMap();
        
        for (var character : characters) {
            elements.put(character.getElementType().getValue(), 0);
        }
        
        // Calculate the amount of characters with the elements
        this.triggerCharacterAchievements(elements, level);
    }
    
    private void triggerCharacterAchievements(ItemParamMap elements, int level) {
        // Get current amount of characters with same level or higher
        int anyCount = 0;
        
        for (var character : this.getCharacterCollection()) {
            if (character.getLevel() >= level) {
                anyCount++;
            } else {
                continue;
            }
            
            int element = character.getElementType().getValue();
            
            if (elements.containsKey(element)) {
                elements.add(element, 1);
            }
        }
        
        // Trigger achievements
        var achievements = this.getPlayer().getAchievementManager();
        
        achievements.trigger(
            AchievementCondition.CharactersWithSpecificLevelAndQuantity,
            anyCount,
            level,
            0
        );
        
        for (var entry : elements) {
            int element = entry.getIntKey();
            int amount = entry.getIntValue();
            
            achievements.trigger(
                AchievementCondition.CharactersWithSpecificNumberLevelAndAttributes,
                amount,
                level,
                element
            );
        }
    }
    
    // Discs

    public GameDisc getDiscById(int id) {
        if (id <= 0) {
            return null;
        }
        
        return this.discs.get(id);
    }
    
    public boolean hasDisc(int id) {
        return this.discs.containsKey(id);
    }
    
    public GameDisc addDisc(int discId) {
        // Sanity check to make sure we dont have this character already
        if (this.hasDisc(discId)) {
            return null;
        }
        
        // Get data
        var data = GameData.getDiscDataTable().get(discId);
        if (data == null) return null;
        
        // Add disc
        return this.addDisc(data);
    }

    public GameDisc addDisc(DiscDef data) {
        // Sanity check to make sure we dont have this disc already
        if (this.hasDisc(data.getId())) {
            return null;
        }
        
        // Prevent players from getting unavliable discs
        if (!data.isAvailable()) {
            return null;
        }
        
        // Create disc
        var disc = new GameDisc(this.getPlayer(), data);
        
        // Save to database
        disc.save();
        
        // Add to discs
        this.discs.put(disc.getDiscId(), disc);
        
        // Set flags for player to update discs in their handbook
        this.setUpdateDiscHandbook(true);
        this.setHasAddedDisc(true);
        
        // Return disc
        return disc;
    }
    
    public Collection<GameDisc> getDiscCollection() {
        return this.getDiscs().values();
    }
    
    public HandbookInfo getDiscHandbook() {
        var bitset = new Bitset();
        
        for (var disc : this.getDiscCollection()) {
            // Get handbook
            var data = GameData.getHandbookDataTable().get(disc.getDiscId());
            if (data == null) continue;
            
            // Set flag
            bitset.setBit(data.getIndex());
        }
        
        var handbook = HandbookInfo.newInstance()
                .setType(2)
                .setData(bitset.toByteArray());
        
        return handbook;
    }
    
    public PlayerChangeInfo limitBreakAllDiscs() {
        // Create variables
        var change = new PlayerChangeInfo();
        var modifiedDiscs = new ArrayList<GameDisc>();
        
        // Try to limit all discs
        for (var disc : this.getDiscCollection()) {
            // Skip if at max stars
            if (disc.getStar() >= 5) {
                continue;
            }
            
            // Get transform id
            var transformId = disc.getData().getTransformItemId();
            var transformCount = this.getPlayer().getInventory().getItemCount(transformId);
            
            if (transformCount <= 0) {
                continue;
            }
            
            // Try to limit break
            var discChange = disc.limitBreak(transformCount);
            
            // Check if limit break was successful
            if (discChange == null) {
                continue;
            }
            
            // Merge any changes from the disc limit break to this one
            change.add(discChange);
            
            // Add to changed discs
            modifiedDiscs.add(disc);
        }
        
        // Success
        return change.setExtraData(modifiedDiscs);
    }
    
    // Contacts
    
    public int getNewPhoneMessageCount() {
        int count = 0;
        
        for (var character : this.getCharacterCollection()) {
            if (character.getContact().hasNew()) {
                count++;
            }
        }
        
        return count;
    }
    
    //

    /**
     * Checks if we should add next packages for player
     */
    public void checkPlayerState() {
        // Check if we need to trigger character achievements
        if (this.hasAddedChar) {
            this.hasAddedChar = false;
            this.getPlayer().trigger(
                AchievementCondition.CharactersWithSpecificQuantityAndRarity,
                getCharacters().size()
            );
            this.getPlayer().trigger(
                AchievementCondition.CharactersWithSpecificQuantityAndRarity,
                (int) getCharacters().values().stream().filter(GameCharacter::isMaster).count(),
                1,
                0
            );
        }
        
        // Check if we need to send handbook update to player
        if (this.updateCharHandbook) {
            this.updateCharHandbook = false;
            this.getPlayer().addNextPackage(
                NetMsgId.handbook_change_notify,
                this.getCharacterHandbook()
            );
        }
        
        // Check if we need to trigger disc achievements
        if (this.hasAddedChar) {
            this.hasAddedChar = false;
            this.getPlayer().trigger(
                AchievementCondition.DiscAcquireSpecificQuantityAndRarity, 
                this.getDiscs().size()
            );
            this.getPlayer().trigger(
                AchievementCondition.DiscAcquireSpecificQuantityAndRarity,
                (int) getDiscs().values().stream().filter(GameDisc::isMaster).count(),
                1, 
                0
            );
        }
        
        // Check if we need to send handbook update to player
        if (this.updateDiscHandbook) {
            this.updateDiscHandbook = false;
            this.getPlayer().addNextPackage(
                NetMsgId.handbook_change_notify,
                this.getDiscHandbook()
            );
        }
    }
    
    // Database
    
    public void loadFromDatabase() {
        var db = Nebula.getGameDatabase();
        
        db.getObjects(GameCharacter.class, "playerUid", getPlayerUid()).forEach(character -> {
            // Get data
            var data = GameData.getCharacterDataTable().get(character.getCharId());
            
            // Validate
            if (data == null) {
                return;
            }
            
            character.setPlayer(this.getPlayer());
            character.setData(data);
            
            // Add to characters
            this.characters.put(character.getCharId(), character);
        });
        
        db.getObjects(GameDisc.class, "playerUid", getPlayerUid()).forEach(disc -> {
            // Get data
            var data = GameData.getDiscDataTable().get(disc.getDiscId());
            if (data == null) return;
            
            disc.setPlayer(this.getPlayer());
            disc.setData(data);
            
            // Add
            this.discs.put(disc.getDiscId(), disc);
        });
    }
}
