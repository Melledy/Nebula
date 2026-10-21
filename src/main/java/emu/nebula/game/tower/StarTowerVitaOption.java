package emu.nebula.game.tower;

import dev.morphia.annotations.Entity;
import emu.nebula.data.GameData;
import emu.nebula.proto.Public.TowerVitaOption;
import emu.nebula.proto.Public.UI32s;
import lombok.Getter;

@Getter
@Entity(useDiscriminator = false)
public class StarTowerVitaOption {
    private int id;
    private int type;

    private Boolean booleanValue;
    private Integer integerValue;
    private int[] listValues;

    @Deprecated // Morphia only
    public StarTowerVitaOption() {

    }

    private StarTowerVitaOption(int id, int type) {
        this.id = id;
        this.type = type;
    }

    public static StarTowerVitaOption fromProto(TowerVitaOption proto) {
        // Get option data so we know which value field to read
        var data = GameData.getTowerVitaOptionDataTable().get(proto.getId());
        if (data == null) {
            return null;
        }

        var option = new StarTowerVitaOption(proto.getId(), data.getOptionType());

        if (data.isSwitch()) {
            if (!proto.hasBoolean()) {
                return null;
            }

            option.booleanValue = proto.getBoolean();
        } else if (data.isInteger()) {
            if (!proto.hasInteger()) {
                return null;
            }

            option.integerValue = proto.getInteger();
        } else if (data.isList()) {
            if (!proto.hasList()) {
                return null;
            }

            option.listValues = proto.getList().getValues().toArray();
        } else {
            return null;
        }

        return option;
    }

    // Proto
    
    public TowerVitaOption toProto() {
        var proto = TowerVitaOption.newInstance()
                .setId(this.getId());

        if (this.getType() == 1) {
            if (this.getBooleanValue() == null) {
                return null;
            }

            proto.setBoolean(this.getBooleanValue());
        } else if (this.getType() >= 2 && this.getType() <= 4) {
            if (this.getIntegerValue() == null) {
                return null;
            }

            proto.setInteger(this.getIntegerValue());
        } else if (this.getType() == 5) {
            if (this.getListValues() == null) {
                return null;
            }

            proto.setList(UI32s.newInstance().addAllValues(this.getListValues()));
        } else {
            return null;
        }

        return proto;
    }
}
