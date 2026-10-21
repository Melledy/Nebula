package emu.nebula.data.resources;

import emu.nebula.data.BaseDef;
import emu.nebula.data.ResourceType;
import lombok.Getter;

@Getter
@ResourceType(name = "TowerVitaOption.json")
public class TowerVitaOptionDef extends BaseDef {
    private int Id;
    private int Categories;
    private int Sort;
    private int OptionType;
    private int OptionGroup;
    private String DefaultValue;

    @Override
    public int getId() {
        return Id;
    }

    public boolean isSwitch() {
        return this.OptionType == 1;
    }

    public boolean isInteger() {
        return this.OptionType >= 2 && this.OptionType <= 4;
    }

    public boolean isList() {
        return this.OptionType == 5;
    }
}
