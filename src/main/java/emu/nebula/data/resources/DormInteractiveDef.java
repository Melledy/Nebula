package emu.nebula.data.resources;

import emu.nebula.data.BaseDef;
import emu.nebula.data.ResourceType;
import lombok.Getter;

@Getter
@ResourceType(name = "DormInteractive.json")
public class DormInteractiveDef extends BaseDef {
    private int Id;
    private int CharId;
    private int FurniturnId;
    private int DormInterType;
    private String AnimationConfig;

    @Override
    public int getId() {
        return Id;
    }
}
