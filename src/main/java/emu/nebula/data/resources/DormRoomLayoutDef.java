package emu.nebula.data.resources;

import emu.nebula.data.BaseDef;
import emu.nebula.data.ResourceType;
import lombok.Getter;

@Getter
@ResourceType(name = "DormRoomLayout.json")
public class DormRoomLayoutDef extends BaseDef {
    private int Id;
    private int RoomLayoutId;
    private int FurnitureId;
    private int[] Pos;
    private int Rot;
    private int ApplyRoomType;

    @Override
    public int getId() {
        return Id;
    }
}
