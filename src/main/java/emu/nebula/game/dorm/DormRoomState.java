package emu.nebula.game.dorm;

import java.util.ArrayList;
import java.util.List;

import dev.morphia.annotations.Entity;
import emu.nebula.proto.PublicDorm.DormRoomInfo;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity(useDiscriminator = false)
public class DormRoomState {
    private int dormRoomId;
    private List<DormFurniture> furnitureList;
    private List<DormRole> roleList;
    private int discId;
    private String roomName;
    private int roomScene;
    private long restNameTime;

    @Deprecated // Morphia only
    public DormRoomState() {

    }

    public DormRoomState(int dormRoomId) {
        this.dormRoomId = dormRoomId;
        this.furnitureList = new ArrayList<>();
        this.roleList = new ArrayList<>();
        this.roomName = "";
    }
    
    // Proto
    
    public DormRoomInfo toProto() {
        var proto = DormRoomInfo.newInstance()
                .setDormRoomId(this.getDormRoomId())
                .setDiscId(this.getDiscId())
                .setRoomName(this.getRoomName() == null ? "" : this.getRoomName())
                .setRoomScene(this.getRoomScene())
                .setRestNameTime(this.getRestNameTime());

        if (this.getFurnitureList() != null) {
            for (var furniture : this.getFurnitureList()) {
                proto.addFurnitureList(furniture.toProto());
            }
        }

        if (this.getRoleList() != null) {
            for (var role : this.getRoleList()) {
                proto.addRoleList(role.toProto());
            }
        }

        return proto;
    }
}
