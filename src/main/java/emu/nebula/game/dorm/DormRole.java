package emu.nebula.game.dorm;

import dev.morphia.annotations.Entity;
import emu.nebula.proto.PublicDorm.DormRoleInfo;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity(useDiscriminator = false)
public class DormRole {
    private int roleId;
    private boolean isVisit;
    private boolean isInvitationRole;

    @Deprecated // Morphia only
    public DormRole() {

    }

    public DormRole(int roleId) {
        this.roleId = roleId;
    }

    public DormRoleInfo toProto() {
        return DormRoleInfo.newInstance()
                .setRoleId(this.roleId)
                .setIsVisit(this.isVisit)
                .setIsInvitationRole(this.isInvitationRole);
    }
}
