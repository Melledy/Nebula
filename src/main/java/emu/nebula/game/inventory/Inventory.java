package emu.nebula.game.inventory;

import java.util.List;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import emu.nebula.GameConstants;
import emu.nebula.Nebula;
import emu.nebula.data.GameData;
import emu.nebula.database.GameDatabaseObject;
import emu.nebula.game.player.PlayerManager;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.Notify.Skin;
import emu.nebula.proto.Public.Honor;
import emu.nebula.proto.Public.Item;
import emu.nebula.proto.Public.Res;
import emu.nebula.proto.Public.Title;
import emu.nebula.proto.Public.UI32;
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
    private IntSet honorList;
    
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
        this.honorList = new IntOpenHashSet();
        
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
    
    public boolean addSkin(int id) {
        // Make sure we are not adding duplicates
        if (this.getExtraSkins().contains(id)) {
            return false;
        }
        
        // Add
        this.getExtraSkins().add(id);
        
        // Save to database
        Nebula.getGameDatabase().addToList(this, this.getUid(), "extraSkins", id);
        
        // Send packet
        this.getPlayer().addNextPackage(
            NetMsgId.character_skin_gain_notify, 
            Skin.newInstance().setNew(UI32.newInstance().setValue(id))
        );
        
        // Success
        return true;
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
    
    public boolean addHeadIcon(int id) {
        // Make sure we are not adding duplicates
        if (this.getHeadIcons().contains(id)) {
            return false;
        }
        
        // Add
        this.getHeadIcons().add(id);
        
        // Save to database
        Nebula.getGameDatabase().addToList(this, this.getUid(), "headIcons", id);
        
        // Success
        return true;
    }
    
    public boolean addTitle(int id) {
        // Make sure we are not adding duplicates
        if (this.getTitles().contains(id)) {
            return false;
        }
        
        // Add
        this.getTitles().add(id);
        
        // Save to database
        Nebula.getGameDatabase().addToList(this, this.getUid(), "titles", id);
        
        // Success
        return true;
    }
    
    public boolean addHonor(int id) {
        // Make sure we are not adding duplicates
        if (this.getHonorList().contains(id)) {
            return false;
        }
        
        // Add
        this.getHonorList().add(id);
        
        // Save to database
        Nebula.getGameDatabase().addToList(this, this.getUid(), "honorList", id);
        
        // Success
        return true;
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
            case Title -> {
                // Cannot remove titles
                if (amount <= 0) {
                    break;
                }
                
                // Get title data
                if (data.getTitleData() == null) {
                    break;
                }
                
                int titleId = data.getTitleData().getId();
                
                // Add title
                if (this.addTitle(titleId)) {
                    // Add to change info
                    var proto = Title.newInstance()
                            .setTitleId(titleId);
                    
                    change.add(proto);
                }
            }
            case Honor -> {
                // Cannot remove honor title
                if (amount <= 0) {
                    break;
                }
                
                // Add honor title
                if (this.addHonor(id)) {
                    // Add to change info
                    var proto = Honor.newInstance()
                            .setNewId(id);
                    
                    change.add(proto);
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
    public synchronized boolean hasItem(int id, int count) {
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
            case CharacterSkin -> {
                yield this.hasSkin(id);
            }
            case Title -> {
                yield this.getTitles().contains(id);
            }
            default -> {
                // Not implemented
                yield false;
            }
        };
        
        //
        return result;
    }
    
    public synchronized boolean hasItems(ItemParamMap params) {
        boolean hasItems = true;
        
        for (var param : params.entries()) {
            hasItems = this.hasItem(param.getIntKey(), param.getIntValue());
            
            if (!hasItems) {
                return hasItems;
            }
        }
        
        return hasItems;
    }
    
    // Utility functions
    
    public PlayerChangeInfo produce(int id, int num, PlayerChangeInfo change) {
        // Init change info
        if (change == null) {
            change = new PlayerChangeInfo();
        }
        
        // Get production data
        var data = GameData.getProductionDataTable().get(id);
        if (data == null) {
            return change;
        }
        
        // Get materials
        var materials = data.getMaterials().mulitply(num);
        
        // Verify that we have the materials
        if (!this.hasItems(materials)) {
            return change;
        }
        
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
        if (!this.hasItem(GameConstants.ENERGY_BUY_ITEM_ID, 30)) {
            return change;
        }
        
        // Remove gems
        this.removeItem(GameConstants.ENERGY_BUY_ITEM_ID, 30, change);
        
        // Add energy
        this.getPlayer().addEnergy(60, change);
        
        // Success
        return change;
    }
    
    public PlayerChangeInfo buyItem(int currencyId, int currencyCount, ItemParamMap buyItems, int buyCount) {
        return this.buyItem(currencyId, currencyCount, buyItems, buyCount, null);
    }
    
    public PlayerChangeInfo buyItem(int currencyId, int currencyCount, ItemParamMap buyItems, int buyCount, PlayerChangeInfo change) {
        // Player change info
        if (change == null) {
            change = new PlayerChangeInfo();
        }
        
        // Sanity check
        if (buyCount <= 0) {
            return change;
        }
        
        // Make sure we have the currency
        int cost = buyCount * currencyCount;
        
        if (!this.hasItem(currencyId, cost)) {
            return change;
        }
        
        // Remove currency item
        this.removeItem(currencyId, cost, change);
        
        // Add items
        this.addItems(buyItems.mulitply(buyCount), change);
        
        // Success
        return change.setSuccess(true);
    }
    
    public PlayerChangeInfo useItem(int id, int count, int selectId, PlayerChangeInfo change) {
        // Player change info
        if (change == null) {
            change = new PlayerChangeInfo();
        }
        
        // Get item data
        var data = GameData.getItemDataTable().get(id);
        if (data == null || data.getUseParams() == null) {
            return change;
        }
        
        // Make sure we have this item
        if (!this.hasItem(id, count)) {
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
            case 3 -> {
                // Selected item
                int selectCount = data.getUseParams().get(selectId);
                
                if (selectCount <= 0) {
                    return change;
                }
                
                // Add selected item
                this.addItem(selectId, selectCount, change);
                
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
        return change.setSuccess(true);
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
