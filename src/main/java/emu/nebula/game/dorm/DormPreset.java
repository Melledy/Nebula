package emu.nebula.game.dorm;

import java.util.ArrayList;
import java.util.List;

import dev.morphia.annotations.Entity;
import emu.nebula.proto.PublicDorm.FurniturePresetInfo;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity(useDiscriminator = false)
public class DormPreset {
    private int buildId;
    private int dormType;
    private String buildName;
    private long createTime;
    private List<DormFurniture> furnitureList;

    @Deprecated // Morphia only
    public DormPreset() {

    }

    public DormPreset(int buildId, int dormType, long createTime) {
        this.buildId = buildId;
        this.dormType = dormType;
        this.createTime = createTime;
        this.buildName = "";
        this.furnitureList = new ArrayList<>();
    }

    public FurniturePresetInfo toProto() {
        var proto = FurniturePresetInfo.newInstance()
                .setBuildId(this.buildId)
                .setDormType(this.dormType)
                .setBuildName(this.buildName == null ? "" : this.buildName)
                .setCreateTime(this.createTime);

        if (this.furnitureList != null) {
            for (var furniture : this.furnitureList) {
                proto.addFurnitureList(furniture.toProto());
            }
        }

        return proto;
    }
}
