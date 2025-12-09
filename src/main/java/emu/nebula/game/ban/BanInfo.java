package emu.nebula.game.ban;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import emu.nebula.Nebula;
import emu.nebula.database.GameDatabaseObject;
import emu.nebula.game.player.PlayerErrorCode;
import emu.nebula.proto.Public.Error;
import emu.nebula.util.Utils;
import lombok.Getter;
import java.util.Date;

@Getter
@Entity(value = "bans", useDiscriminator = false)
public class BanInfo implements GameDatabaseObject {
    @Id
    private String id;
    
    private int playerUid;
    private long startTime;
    private long endTime;
    private String reason;
    private String bannedBy;
    private String ipAddress;

    @Deprecated // Morphia only
    public BanInfo() {
    }
    
    public BanInfo(int playerUid, long endTime, String reason, String bannedBy, String ipAddress) {
        this.playerUid = playerUid;
        this.startTime = System.currentTimeMillis();
        this.endTime = endTime;
        this.reason = reason;
        this.bannedBy = bannedBy;
        this.ipAddress = ipAddress;
        // Generate ID based on either player UID or IP address
        this.id = (ipAddress != null && !ipAddress.isEmpty() && playerUid == 0) ? "ip_" + ipAddress :
                "player_" + playerUid;
    }
    
    public boolean isExpired() {
        return endTime != 0 && endTime < System.currentTimeMillis();
    }
    
    public String getExpirationDateString() {
        if (endTime == 0) {
            return "Never";
        }
        return Utils.formatTimestamp(this.endTime);
    }
    
    @Override
    public void save() {
        GameDatabaseObject.super.save();
    }

    public Error toProto() {
        return Error.newInstance()
                .setCode(PlayerErrorCode.ErrBan.getValue())
                .addArguments(
                        getExpirationDateString() + "\n" +
                                (this.reason != null ? "\n (" + this.reason + ")" : "\n" + this.id));
    }
}