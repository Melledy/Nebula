package emu.nebula.game.inventory;

import java.util.List;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import emu.nebula.GameConstants;
import emu.nebula.Nebula;
import emu.nebula.data.GameData;
import emu.nebula.database.GameDatabaseObject;
import emu.nebula.game.player.PlayerManager;
import emu.nebula.proto.Public.Item;
import emu.nebula.proto.Public.Res;
import emu.nebula.game.player.Player;
import emu.nebula.game.player.PlayerChangeInfo;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import lombok.Getter;

@Getter
@Entity(value = "inventory", useDiscriminator = false)
public class Inventory extends PlayerManager implements GameDatabaseObject {
    @Id
    private int uid;
    
    // Database persistent data
    private IntSet extraSkins;
    private IntSet headIcons;
    private IntSet titles;
    
    // Items/resources
    private transient Int2ObjectMap<GameResource> resources;
    private transient Int2ObjectMap<GameItem> items;
    
    public Inventory() {
        this.resources = new Int2ObjectOpenHashMap<>();
        this.items = new Int2ObjectOpenHashMap<>();
    }
    
    public Inventory(Player player) {
        this();
        this.setPlayer(player);
        this.uid = player.getUid();
        
        // Setup
        this.extraSkins = new IntOpenHashSet();
        this.headIcons = new IntOpenHashSet();
        this.titles = new IntOpenHashSet();
        
        // Add titles directly
        this.getTitles().add(player.getTitlePrefix());
        this.getTitles().add(player.getTitleSuffix());
        
        // Save to database
        this.save();
    }
    
    //
    
    public IntCollection getAllSkinIds() {
        // Setup int collection
        var skins = new IntOpenHashSet();
        
        // Add character skins
        for (var character : getPlayer().getCharacters().getCharacterCollection()) {
            // Add default skin id
            skins.add(character.getData().getDefaultSkinId());
            
            // Add advance skin
            if (character.getAdvance() >= character.getData().getAdvanceSkinUnlockLevel()) {
                skins.add(character.getData().getAdvanceSkinId());
            }
        }
        
        // Finally, add extra skins
        skins.addAll(this.getExtraSkins());
        
        // Complete and return
        return skins;
    }
    
    public boolean hasSkin(int id) {
        // Get skin data
        var skinData = GameData.getCharacterSkinDataTable().get(id);
        if (skinData == null) {
            return false;
        }
        
        // Get character
        var character = getPlayer().getCharacters().getCharacterById(skinData.getCharId());
        if (character == null) {
            return false;
        }
        
        // Check
        switch (skinData.getType()) {
            case 1:
                // Default skin, always allow
                break;
            case 2:
                // Ascension skin, only allow if the character has the right ascension level
                if (character.getAdvance() < character.getData().getAdvanceSkinUnlockLevel()) {
                    return false;
                }
                break;
            default:
                // Extra skin, only allow if we have the skin unlocked
                if (!getPlayer().getInventory().getExtraSkins().contains(id)) {
                    return false;
                }
                break;
        }
        
        // Unknown
        return true;
    }
    
    public void addSkin(int id) {
        // Make sure we are not adding duplicates
        if (this.getExtraSkins().contains(id)) {
            return;
        }
        
        // Add
        this.getExtraSkins().add(id);
        
        // Save to database
        Nebula.getGameDatabase().addToList(this, this.getUid(), "extraSkins", id);
    }
    
    public IntCollection getAllHeadIcons() {
        // Setup int collection
        var icons = new IntOpenHashSet();
        
        // Add character skins
        for (var character : getPlayer().getCharacters().getCharacterCollection()) {
            // Add default skin id
            icons.add(character.getData().getDefaultSkinId());
            
            // Add advance skin
            if (character.getAdvance() >= character.getData().getAdvanceSkinUnlockLevel()) {
                icons.add(character.getData().getAdvanceSkinId());
            }
        }
        
        // Finally, add extra skins
        icons.addAll(this.getHeadIcons());
        
        // Complete and return
        return icons;
    }
    
    public boolean hasHeadIcon(int id) {
        // Get head icon data
        var data = GameData.getPlayerHeadDataTable().get(id);
        if (data == null) {
            return false;
        }
        
        // Check to make sure we own this head icon
        if (data.getHeadType() == 3) {
            // Character skin icon
            return this.hasSkin(id);
        } else {
            // Extra head icon
            if (!this.getHeadIcons().contains(id)) {
                return false;
            }
        }
        
        // Unknown
        return true;
    }
    
    public void addHeadIcon(int id) {
        // Make sure we are not adding duplicates
        if (this.getHeadIcons().contains(id)) {
            return;
        }
        
        // Add
        this.getHeadIcons().add(id);
        
        // Save to database
        Nebula.getGameDatabase().addToList(this, this.getUid(), "headIcons", id);
    }
    
    public void addTitle(int id) {
        // Make sure we are not adding duplicates
        if (this.getTitles().contains(id)) {
            return;
        }
        
        // Add
        this.getTitles().add(id);
        
        // Save to database
        Nebula.getGameDatabase().addToList(this, this.getUid(), "titles", id);
    }
    
    // Resources
    
    public synchronized int getResourceCount(int id) {
        var res = this.resources.get(id);
        return res != null ? res.getCount() : 0;
    }
    
    // Items
    
    public synchronized int getItemCount(int id) {
        var item = this.getItems().get(id);
        return item != null ? item.getCount() : 0;
    }
    
    // Add/Remove items
    
    public PlayerChangeInfo addItem(int id, int count) {
        return this.addItem(id, count, null);
    }
    
    public synchronized PlayerChangeInfo addItem(int id, int count, PlayerChangeInfo change) {
        // Changes
        if (change == null) {
            change = new PlayerChangeInfo();
        }
        
        // Sanity
        if (count == 0) {
            return change;
        }
        
        // Get game data
        var data = GameData.getItemDataTable().get(id);
        if (data == null) {
            return change;
        }
        
        // Set amount
        int amount = count;
        
        // Add item
        switch (data.getItemType()) {
            case Res -> {
                var res = this.resources.get(id);
                int diff = 0;
                
                if (amount > 0) {
                    // Add resource
                    if (res == null) {
                        res = new GameResource(this.getPlayer(), id, amount);
                        this.resources.put(res.getResourceId(), res);
                        
                        diff = amount;
                    } else {
                        diff = res.add(amount);
                    }
                    
                    res.save();
                } else {
                    // Remove resource
                    if (res == null) {
                        break;
                    }
                    
                    diff = res.add(amount);
                    res.save();
                    
                    if (res.getCount() < 0) {
                        this.resources.remove(id);
                    }
                }
                
                if (diff != 0) {
                    var proto = Res.newInstance()
                            .setTid(id)
                            .setQty(diff);
                    
                    change.add(proto);
                }
            }
            case Item -> {
                var item = this.items.get(id);
                int diff = 0;
                
                if (amount > 0) {
                    // Add resource
                    if (item == null) {
                        item = new GameItem(this.getPlayer(), id, amount);
                        this.items.put(item.getItemId(), item);
                        
                        diff = amount;
                    } else {
                        diff = item.add(amount);
                    }
                    
                    item.save();
                } else {
                    // Remove resource
                    if (item == null) {
                        break;
                    }
                    
                    diff = item.add(amount);
                    item.save();
                    
                    if (item.getCount() < 0) {
                        this.resources.remove(id);
                    }
                }
                
                if (diff != 0) {
                    var proto = Item.newInstance()
                            .setTid(id)
                            .setQty(diff);
                    
                    change.add(proto);
                }
            }
            case Disc -> {
                if (amount <= 0) {
                    break;
                }
                
                var disc = getPlayer().getCharacters().addDisc(id);
                
                if (disc != null) {
                    change.add(disc.toProto());
                }
            }
            case Char -> {
                if (amount <= 0) {
                    break;
                }
                
                var character = getPlayer().getCharacters().addCharacter(id);

                if (character != null) {
                    change.add(character.toProto());
                }
            }
            case Energy -> {
                this.getPlayer().addEnergy(amount, change);
            }
            case WorldRankExp -> {
                this.getPlayer().addExp(amount, change);
            }
            case CharacterSkin -> {
                // Cannot remove skins
                if (amount <= 0) {
                    break;
                }
                
                // Get skin data
                var skinData = GameData.getCharacterSkinDataTable().get(id);
                if (skinData == null) {
                    break;
                }
                
                // Check
                if (skinData.getType() >= 3) {
                    this.addSkin(id);
                }
            }
            default -> {
                // Not implemented
            }
        }
        
        //
        return change;
    }

    @Deprecated
    public synchronized PlayerChangeInfo addItems(List<ItemParam> params, PlayerChangeInfo change) {
        // Changes
        if (change == null) {
            change = new PlayerChangeInfo();
        }
        
        for (ItemParam param : params) {
            this.addItem(param.getId(), param.getCount(), change);
        }
        
        return change;
    }
    
    public synchronized PlayerChangeInfo addItems(ItemParamMap params) {
        return this.addItems(params, null);
    }
    
    public synchronized PlayerChangeInfo addItems(ItemParamMap params, PlayerChangeInfo change) {
        // Changes
        if (change == null) {
            change = new PlayerChangeInfo();
        }
        
        // Sanity
        if (params == null || params.isEmpty()) {
            return change;
        }
        
        // Add items
        for (var param : params.entries()) {
            this.addItem(param.getIntKey(), param.getIntValue(), change);
        }
        
        return change;
    }
    
    public PlayerChangeInfo removeItem(int id, int count) {
        return this.removeItem(id, count, null);
    }
    
    public synchronized PlayerChangeInfo removeItem(int id, int count, PlayerChangeInfo change) {
        if (count > 0) {
            count = -count;
        }
        
        return this.addItem(id, count, change);
    }
    
    public synchronized PlayerChangeInfo removeItems(ItemParamMap params) {
        return this.removeItems(params, null);
    }
    
    public synchronized PlayerChangeInfo removeItems(ItemParamMap params, PlayerChangeInfo change) {
        // Changes
        if (change == null) {
            change = new PlayerChangeInfo();
        }
        
        // Sanity
        if (params == null || params.isEmpty()) {
            return change;
        }
        
        // Remove items
        for (var param : params.entries()) {
            this.removeItem(param.getIntKey(), param.getIntValue(), change);
        }
        
        return change;
    }
    
    /**
     * Checks if the player has enough quanity of this item
     */
    public synchronized boolean verifyItem(int id, int count) {
        // Sanity check
        if (count == 0) {
            return true;
        } else if (count < 0) {
            // Return false if we are trying to verify negative numbers
            return false;
        }
        
        // Get game data
        var data = GameData.getItemDataTable().get(id);
        if (data == null) {
            return false;
        }
        
        boolean result = switch (data.getItemType()) {
            case Res -> {
                yield this.getResourceCount(id) >= count;
            }
            case Item -> {
                yield this.getItemCount(id) >= count;
            }
            case Disc -> {
                yield getPlayer().getCharacters().hasDisc(id);
            }
            case Char -> {
                yield getPlayer().getCharacters().hasCharacter(id);
            }
            default -> {
                // Not implemented
                yield false;
            }
        };
        
        //
        return result;
    }
    
    public synchronized boolean verifyItems(ItemParamMap params) {
        boolean hasItems = true;
        
        for (var param : params.entries()) {
            hasItems = this.verifyItem(param.getIntKey(), param.getIntValue());
            
            if (!hasItems) {
                return hasItems;
            }
        }
        
        return hasItems;
    }
    
    // Utility functions
    
    public PlayerChangeInfo produce(int id, int num) {
        // Get production data
        var data = GameData.getProductionDataTable().get(id);
        if (data == null) {
            return null;
        }
        
        // Get materials
        var materials = data.getMaterials().mulitply(num);
        
        // Verify that we have the materials
        if (!this.verifyItems(materials)) {
            return null;
        }
        
        // Create change info
        var change = new PlayerChangeInfo();
        
        // Remove items
        this.removeItems(materials, change);
        
        // Add produced items
        this.addItem(data.getProductionId(), data.getProductionPerBatch() * num, change);
        
        // Success
        return change.setSuccess(true);
    }
    
    public PlayerChangeInfo buyEnergy() {
        // Create change info
        var change = new PlayerChangeInfo();
        
        // Make sure we have the gems
        if (!this.verifyItem(GameConstants.ENERGY_BUY_ITEM_ID, 30)) {
            return change;
        }
        
        // Remove gems
        this.removeItem(GameConstants.ENERGY_BUY_ITEM_ID, 30, change);
        
        // Add energy
        this.getPlayer().addEnergy(60, change);
        
        // Success
        return change;
    }
    
    public PlayerChangeInfo useItem(int id, int count, PlayerChangeInfo change) {
        // Changes
        if (change == null) {
            change = new PlayerChangeInfo();
        }
        
        // Get item data
        var data = GameData.getItemDataTable().get(id);
        if (data == null || data.getUseParams() == null) {
            return change;
        }
        
        // Make sure we have this item
        if (!this.verifyItem(id, count)) {
            return change;
        }
        
        // Success
        boolean success = false;
        
        // Apply use
        switch (data.getUseAction()) {
            case 2 -> {
                // Add items
                this.addItems(data.getUseParams(), change);
                // Success
                success = true;
            }
            default -> {
                // Not implemented
            }
        }

        // Consume item if successful
        if (success) {
            this.removeItem(id, count, change);
        }
        
        // Success
        return change;
    }
    
    // Database
    
    public void loadFromDatabase() {
        var db = Nebula.getGameDatabase();
        
        db.getObjects(GameItem.class, "playerUid", getPlayerUid()).forEach(item -> {
            // Get data
            var data = GameData.getItemDataTable().get(item.getItemId());
            if (data == null) return;
            
            // Add
            this.items.put(item.getItemId(), item);
        });
        
        db.getObjects(GameResource.class, "playerUid", getPlayerUid()).forEach(res -> {
            // Get data
            var data = GameData.getItemDataTable().get(res.getResourceId());
            if (data == null) return;
            
            // Add
            this.resources.put(res.getResourceId(), res);
        });
    }
}
