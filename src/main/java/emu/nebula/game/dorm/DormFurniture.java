package emu.nebula.game.dorm;

import dev.morphia.annotations.Entity;
import emu.nebula.proto.PublicDorm.FurnitureInfo;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity(useDiscriminator = false)
public class DormFurniture {
    private int furnitureId;
    private int x;
    private int y;
    private int z;
    private int rotation;

    @Deprecated // Morphia only
    public DormFurniture() {

    }

    public DormFurniture(int furnitureId, int x, int y, int z, int rotation) {
        this.furnitureId = furnitureId;
        this.x = x;
        this.y = y;
        this.z = z;
        this.rotation = rotation;
    }

    public DormFurniture(FurnitureInfo info) {
        this.furnitureId = info.getFurnitureId();
        this.x = info.getX();
        this.y = info.getY();
        this.z = info.getZ();
        this.rotation = info.getRotation();
    }

    // Proto
    
    public FurnitureInfo toProto() {
        return FurnitureInfo.newInstance()
                .setFurnitureId(this.furnitureId)
                .setX(this.x)
                .setY(this.y)
                .setZ(this.z)
                .setRotation(this.rotation);
    }
}
