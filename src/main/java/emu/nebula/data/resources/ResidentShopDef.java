package emu.nebula.data.resources;

import emu.nebula.Nebula;
import emu.nebula.data.BaseDef;
import emu.nebula.data.ResourceType;
import emu.nebula.util.Utils;
import lombok.Getter;

@Getter
@ResourceType(name = "ResidentShop.json")
public class ResidentShopDef extends BaseDef {
    private int Id;
    private int RefreshTimeType;
    private int RefreshInterval;
    private String OpenTime;

    private transient long openTimeSeconds;

    @Override
    public int getId() {
        return Id;
    }

    /**
     * Returns the next refresh time for this resident shop in epoch seconds.
     */
    public long getNextRefreshTime() {
        if (this.openTimeSeconds > 0 && Nebula.getCurrentServerTime() < this.openTimeSeconds) {
            return this.openTimeSeconds;
        }

        return Utils.getNextResetTimeSeconds(this.RefreshTimeType);
    }

    /**
     * Returns whether this resident shop should currently be visible to the client.
     */
    public boolean isVisible() {
        return this.openTimeSeconds <= 0 || this.openTimeSeconds <= Nebula.getCurrentServerTime();
    }

    @Override
    public void onLoad() {
        this.openTimeSeconds = Utils.dateToSeconds(this.OpenTime);
    }

}
