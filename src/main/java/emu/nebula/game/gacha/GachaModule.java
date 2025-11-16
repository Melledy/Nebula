package emu.nebula.game.gacha;

import emu.nebula.data.GameData;
import emu.nebula.game.GameContext;
import emu.nebula.game.GameContextModule;
import emu.nebula.game.inventory.ItemAcquireMap;
import emu.nebula.game.inventory.ItemParamMap;
import emu.nebula.game.inventory.ItemType;
import emu.nebula.game.player.Player;
import emu.nebula.game.player.PlayerChangeInfo;
import emu.nebula.proto.Public.Transform;
import it.unimi.dsi.fastutil.ints.IntArrayList;

public class GachaModule extends GameContextModule {

    public GachaModule(GameContext context) {
        super(context);
    }

    public GachaResult spin(Player player, int bannerId, int mode) {
        // Get pull count
        int amount = mode == 2 ? 10 : 1;
        
        // Get banner data
        var data = GameData.getGachaDataTable().get(bannerId);
        if (data == null) {
            return null;
        }
        
        var bannerStorage = data.getStorageData();
        if (bannerStorage == null) {
            return null;
        }
        
        // Create change info
        var change = new PlayerChangeInfo();
        
        // Check if we have the materials to gacha TODO
        int costQty = player.getInventory().getItemCount(bannerStorage.getDefaultId());
        int costReq = bannerStorage.getDefaultQty() * amount;
        
        if (costReq > costQty) {
            // Not enough materials, check if we can convert
            int convertQty = player.getInventory().getResourceCount(bannerStorage.getCostId());
            int convertReq = bannerStorage.getCostQty() * (costReq - costQty);
            
            // Check if we can buy pulls
            if (convertReq > convertQty) {
                return null;
            }
            
            // Convert to pull currency
            player.getInventory().removeItem(bannerStorage.getCostId(), convertReq, change);
        }
        
        // Consume pull currency
        player.getInventory().removeItem(bannerStorage.getDefaultId(), Math.min(costReq, costQty), change);
        
        // Get gacha banner info
        var info = player.getGachaManager().getBannerInfo(data);
        
        // Do gacha
        var results = new IntArrayList();
        
        for (int i = 0; i < amount; i++) {
            int id = info.doPull(data);
            if (id <= 0) continue;
            
            results.add(id);
        }
        
        // Setup variables
        var acquireItems = new ItemAcquireMap(player, results);
        var transformItemsSrc = new ItemParamMap();
        var transformItemsDst = new ItemParamMap();
        var bonusItems = new ItemParamMap();
        
        // Add for player
        for (var entry : acquireItems.getItems().int2ObjectEntrySet()) {
            // Get ids and aquire params
            int id = entry.getIntKey();
            var acquire = entry.getValue();
            
            // Add to player
            if (acquire.getType() == ItemType.Char) {
                // Get add amount
                int count = acquire.getCount();
                
                // Add char to player
                if (acquire.getBegin() == 0) {
                    player.getInventory().addItem(id, 1, change);
                    count--;
                }
                
                // Talent material
                if (count > 0) {
                    var characterData = GameData.getCharacterDataTable().get(id);
                    if (characterData == null) continue;
                    
                    transformItemsSrc.add(id, count);
                    transformItemsDst.add(characterData.getFragmentsId(), characterData.getTransformQty() * count);
                    transformItemsDst.add(24, 40 * count); // Expert permits
                }
            } else if (acquire.getType() == ItemType.Disc) {
                // Get add amount
                int begin = acquire.getBegin();
                int count = acquire.getCount();
                
                // Add disc to player
                if (begin == 0) {
                    player.getInventory().addItem(id, 1, change);
                    count--;
                    begin++;
                }
                
                // Talent material
                int maxTransformCount = Math.max(6 - begin, 0);
                int transformCount = Math.min(count, maxTransformCount);
                int extraCount = count - maxTransformCount;
                
                // Transform
                if (transformCount > 0) {
                    var discData = GameData.getDiscDataTable().get(id);
                    if (discData == null) continue;
                    
                    // Star material
                    transformItemsSrc.add(id, transformCount);
                    transformItemsDst.add(discData.getTransformItemId(), transformCount);
                } else if (extraCount > 0) {
                    // Permit
                    transformItemsSrc.add(id, extraCount);
                    transformItemsDst.add(23, 100 * extraCount);
                }
                
                // Add Travel permits
                bonusItems.add(23, 100 * acquire.getCount());
            } else {
                // Should never happen
                bonusItems.add(id, acquire.getCount());
            }
            
            // Add gold discs
            bonusItems.add(602, 30 * acquire.getCount());
        }
        
        // Add transform items to extra items
        bonusItems.add(transformItemsDst); // Add transform items
        
        // Add extra items
        player.getInventory().addItems(bonusItems, change);
        
        // Add acquire/transform protos
        change.add(acquireItems.toProto());
        
        var transform = Transform.newInstance();
        transformItemsSrc.toItemTemplateStream().forEach(transform::addSrc);
        transformItemsDst.toItemTemplateStream().forEach(transform::addDst);
        change.add(transform);
        
        // Save banner info to database
        player.getGachaManager().saveBanner(info);
        
        // Add history
        var log = new GachaHistoryLog(data.getGachaType(), results);
        player.getGachaManager().addGachaHistory(log);
        
        // Complete
        return new GachaResult(info, change, results);
    }
}
