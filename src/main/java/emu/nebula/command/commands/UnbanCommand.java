package emu.nebula.command.commands;

import emu.nebula.Nebula;
import emu.nebula.command.Command;
import emu.nebula.command.CommandArgs;
import emu.nebula.command.CommandHandler;
import emu.nebula.game.ban.BanInfo;
import emu.nebula.game.player.Player;

import java.util.Locale;

@Command(label = "unban", permission = "admin.ban", desc = """
        !unban {all | ip | uid} [player uid | ip] = Unbans a player or IP address.
        - all unbans both the player and their IP by UID, so use a UID as the second argument
        - uid unbans only the UID, so use a UID as the second argument
        - ip unbans only the IP address, so use an IP as the second argument""")
public class UnbanCommand implements CommandHandler {

    @Override
    public String execute(CommandArgs args) {
        if (args.size() < 2) {
            return "Invalid amount of args";
        }

        int unbannedUid = 0;
        String unbannedIp = null;

        String mode = args.get(0).toLowerCase(Locale.ROOT);

        if (!mode.equals("all") && !mode.equals("uid") && !mode.equals("ip"))
            return "Unable to parse mode.";

        if (mode.equals("ip")) {
            unbannedIp = args.get(1);
        } else {
            try {
                unbannedUid = Integer.parseInt(args.get(1));
            } catch (NumberFormatException ignored) {
                return "Unable to parse uid.";
            }
        }

        var banModule = Nebula.getGameContext().getBanModule();

        Player player = null;
        if (!mode.equals("ip")) {
            player = Nebula.getGameContext().getPlayerModule().getPlayer(unbannedUid);

            if (player == null) {
                return "Failed, player not found.";
            }
        }

        switch (mode) {
            case "all" -> {
                BanInfo banInfo = banModule.getPlayerBanInfo(player.getUid());

                banModule.unbanPlayer(player.getUid());

                if (banInfo != null) {
                    unbannedIp = banInfo.getIpAddress();
                    if (unbannedIp != null)
                        banModule.unbanIp(unbannedIp);
                }
                return "Unban a player all mode " + unbannedUid;
            } case "uid" -> {
                banModule.unbanPlayer(player.getUid());
                return "Unban a player " + unbannedUid;
            } case "ip" -> {
                banModule.unbanIp(unbannedIp);
                return "Unban a ip " + unbannedIp;
            }
            default -> {
                // Fallback
                return "Unban sub command not found";
            }
        }
    }
}
