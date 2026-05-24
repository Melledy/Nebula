package emu.nebula.game.inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import emu.nebula.proto.Public.Item;
import emu.nebula.proto.Public.ItemInfo;
import emu.nebula.proto.Public.ItemTpl;
import emu.nebula.util.JsonUtils;

import it.unimi.dsi.fastutil.ints.Int2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.objects.ObjectBidirectionalIterable;
import it.unimi.dsi.fastutil.objects.ObjectBidirectionalIterator;

import us.hebi.quickbuf.RepeatedMessage;

public class ItemParamMap extends Int2IntLinkedOpenHashMap implements ObjectBidirectionalIterable<Int2IntMap.Entry> {
    private static final long serialVersionUID = -4186524272780523459L;
    
    public static final ItemParamMap EMPTY = new ItemParamMap() {
        public int put(int itemId, int count) {
            return 0;
        }
        
        public int add(int itemId, int count) {
            return 0;
        }
    };
    
    @Override @Deprecated
    public int addTo(int itemId, int count) {
        return this.add(itemId, count);
    }
    
    public int add(int itemId, int count) {
        if (count == 0) {
            return 0;
        }
        
        return super.addTo(itemId, count);
    }
    
    /**
     * Adds all item params from the other map to this one
     * @param map The other item param map
     */
    public void add(ItemParamMap map) {
        for (var entry : map.entries()) {
            this.add(entry.getIntKey(), entry.getIntValue());
        }
    }
    
    /**
     * Returns a new ItemParamMap with item amounts multiplied
     * @param multiplier Value to multiply all item amounts in this map by
     * @return ItemParamMap
     */
    public ItemParamMap multiply(int multiplier) {
        var params = new ItemParamMap();
        
        for (var entry : this.int2IntEntrySet()) {
            params.put(entry.getIntKey(), entry.getIntValue() * multiplier);
        }
        
        return params;
    }
    
    @Override
    public ItemParamMap clone() {
        var map = new ItemParamMap();
        
        for (var entry : this) {
            map.put(entry.getIntKey(), entry.getIntValue());
        }
        
        return map;
    }
    
    // Iterable

    @Override
    public ObjectBidirectionalIterator<Int2IntMap.Entry> iterator() {
        return this.int2IntEntrySet().iterator();
    }
    
    public FastEntrySet entries() {
        return this.int2IntEntrySet();
    }
    
    // Converters (lists/streams)
    
    public List<ItemParam> toList() {
        List<ItemParam> list = new ArrayList<>();
        
        for (var entry : this.int2IntEntrySet()) {
            list.add(new ItemParam(entry.getIntKey(), entry.getIntValue()));
        }
        
        return list;
    }

    public Stream<ItemTpl> toItemTemplateStream() {
        return entries()
                .stream()
                .map(e -> ItemTpl.newInstance().setTid(e.getIntKey()).setQty(e.getIntValue()));
    }
    
    public Stream<Item> toItemProtoStream() {
        return entries()
                .stream()
                .map(e -> Item.newInstance().setTid(e.getIntKey()).setQty(e.getIntValue()));
    }
    
    // Helpers

    public static ItemParamMap fromTemplates(RepeatedMessage<ItemTpl> items) {
        var map = new ItemParamMap();
        
        for (var template : items) {
            map.add(template.getTid(), template.getQty());
        }
        
        return map;
    }

    public static ItemParamMap fromItemInfos(RepeatedMessage<ItemInfo> items) {
        var map = new ItemParamMap();
        
        for (var template : items) {
            map.add(template.getTid(), template.getQty());
        }
        
        return map;
    }

    public static ItemParamMap fromItemParams(List<ItemParam> items) {
        var map = new ItemParamMap();
        
        for (var template : items) {
            map.add(template.getId(), template.getCount());
        }
        
        return map;
    }

    public static ItemParamMap fromJsonString(String jsonString) {
        var map = new ItemParamMap();
        var json = JsonUtils.decodeMap(jsonString, Integer.class, Integer.class);
        
        for (var entry : json.entrySet()) {
            map.add(entry.getKey(), entry.getValue());
        }
        
        return map;
    }
    
    // Static helpers
    
    public static ItemParamMap of(int... entries) {
        // Create
        var params = new ItemParamMap();
        
        // Add items
        for (int i = 0; i < entries.length; i++) {
            // Get intial item id
            int itemId = entries[i];
            int count = 0;
            
            // Increment
            i++;
            
            // Get count
            if (i < entries.length) {
                count = entries[i];
                
                // Add to item param map
                params.add(itemId, count);
            }
        }
        
        return params;
    }
}
