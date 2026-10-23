package emu.nebula.data.resources;

import emu.nebula.data.BaseDef;
import emu.nebula.data.GameData;
import emu.nebula.data.ResourceType;
import emu.nebula.util.Utils;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import lombok.Getter;

@Getter
@ResourceType(name = "DropPkg.json")
public class DropPkgDef extends BaseDef {
    private int PkgId;
    private int ItemId;
    
    private static Int2ObjectMap<IntList> PACKAGES = new Int2ObjectOpenHashMap<>();
    
    @Override
    public int getId() {
        return PkgId;
    }
    
    @Override
    public void onLoad() {
        var packageList = PACKAGES.computeIfAbsent(this.PkgId, i -> new IntArrayList());
        packageList.add(this.ItemId);
    }
    
    public static int getRandomDrop(int packageId) {
        var packageList = PACKAGES.get(packageId);
        
        if (packageList == null) {
            // Try and get package id from drop data
            var dropData = GameData.getDropDataTable().get(packageId);
            if (dropData != null) {
                packageList = PACKAGES.get(dropData.getPkgId());
            }
        }
        
        if (packageList == null) {
            return 0;
        }
        
        return Utils.randomElement(packageList);
    }
}
