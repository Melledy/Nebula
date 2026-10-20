package emu.nebula.data.resources;

import emu.nebula.data.BaseDef;
import emu.nebula.data.ResourceType;
import lombok.Getter;

@Getter
@ResourceType(name = "DormRoom.json")
public class DormRoomDef extends BaseDef {
    private int Id;
    private int RoomType;
    private int RoomSizeType;
    private int SceneRes;
    private String DefaultName;
    private int DefaultLayoutId;
    private boolean DefaultCanUse;
    private int RoleNum;
    private int FloorRatio;

    @Override
    public int getId() {
        return Id;
    }
}
