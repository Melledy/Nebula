package emu.nebula.data.resources;

import emu.nebula.data.BaseDef;
import emu.nebula.data.ResourceType;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import lombok.Getter;

@Getter
@ResourceType(name = "GachaType.json")
public class GachaTypeDef extends BaseDef {
    private int Id;
    private IntArrayList CoinItem;

    @Override
    public int getId() {
        return this.Id;
    }

    @Override
    public void onLoad() {
        if (this.CoinItem == null) {
            this.CoinItem = new IntArrayList();
        }
    }

    public boolean containsCoinItem(int itemId) {
        return this.CoinItem != null && this.CoinItem.contains(itemId);
    }

}
