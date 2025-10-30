package emu.nebula.game.inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import emu.nebula.proto.Public.Item;
import emu.nebula.proto.Public.ItemInfo;
import emu.nebula.proto.Public.ItemTpl;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import us.hebi.quickbuf.RepeatedMessage;

public class ItemParamMap extends Int2IntOpenHashMap {
    private static final long serialVersionUID = -4186524272780523459L;

    public FastEntrySet entries() {
        return this.int2IntEntrySet();
    }
    
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
     * @param mult Value to multiply all item amounts in this map by
     * @return
     */
    public ItemParamMap mulitply(int multiplier) {
        var params = new ItemParamMap();
        
        for (var entry : this.int2IntEntrySet()) {
            params.put(entry.getIntKey(), entry.getIntValue() * multiplier);
        }
        
        return params;
    }
    
    //
    
    public FastEntrySet getEntrySet() {
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
        return getEntrySet()
                .stream()
                .map(e -> ItemTpl.newInstance().setTid(e.getIntKey()).setQty(e.getIntValue()));
    }
    
    public Stream<Item> toItemProtoStream() {
        return getEntrySet()
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
}
