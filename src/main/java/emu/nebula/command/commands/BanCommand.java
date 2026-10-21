package emu.nebula.command.commands;

import emu.nebula.Nebula;
import emu.nebula.command.Command;
import emu.nebula.command.CommandArgs;
import emu.nebula.command.CommandHandler;
import emu.nebula.game.player.Player;
import emu.nebula.util.Utils;

import java.util.Locale;

@Command(label = "ban",
        permission = "admin.ban",
        desc = """
                !ban {all | ip | uid} [player uid | ip] (end timestamp in ms) (reason) = Bans a player or IP address.
                - all bans both the player and their IP by UID, so use a UID as the second argument
                - uid bans only the UID, so use a UID as the second argument
                - ip bans only the IP address, so use an IP as the second argument
                - Omit the end timestamp for a permanent ban""")
public class BanCommand implements CommandHandler {

    @Override
    public String execute(CommandArgs args) {
        if (args.size() < 2) {
            return "Invalid amount of args";
        }

        int bannedUid = 0;
        long banEndTime = 0;
        String bannedIp = null;
        String reason = null;

        String mode = args.get(0).toLowerCase(Locale.ROOT);

        switch (args.size()) {
            case 4:
                reason = args.get(3);
            case 3: {
                try {
                    banEndTime = Long.parseLong(args.get(2));
                } catch (NumberFormatException ignored) {
                    return "Unable to parse timestamp.";
                }
            }
            case 2: {
                if (mode.equals("ip")) {
                    bannedIp = args.get(1);
                } else {
                    try {
                        bannedUid = Integer.parseInt(args.get(1));
                    } catch (NumberFormatException ignored) {
                        return "Unable to parse uid.";
                    }
                }
            }
            case 1: {
                if (!mode.equals("all") && !mode.equals("uid") && !mode.equals("ip"))
                    return "Unable to parse mode.";
            }
            default: break;
        }

        if (banEndTime != 0 && banEndTime < System.currentTimeMillis()) {
            return "Failed, the end timestamp must be greater than the current time";
        }

        var banModule = Nebula.getGameContext().getBanModule();

        Player player;
        if (!mode.equals("ip")) {
            player = Nebula.getGameContext().getPlayerModule().getPlayer(bannedUid);

            if (player == null) {
                return "Failed, player not found.";
            }
        }

        switch (mode) {
            case "all" -> {
                banModule.banPlayer(
                        bannedUid,
                        banEndTime,
                        reason,
                        true,
                        args.getSender() != null ? String.valueOf(args.getSender().getUid()) : "Console");
                return "Banned player all mode " + bannedUid + " until " + Utils.formatTimestamp(banEndTime) +
                        (reason != null ? " (" + reason + ")" : "");
            }
            case "ip" -> {
                banModule.banIp(
                        bannedIp,
                        banEndTime,
                        reason,
                        args.getSender() != null ? String.valueOf(args.getSender().getUid()) : "Console");
                return "Banned ip " + bannedIp + " until " + Utils.formatTimestamp(banEndTime) +
                        (reason != null ? " (" + reason + ")" : "");
            }
            case "uid" -> {
                banModule.banPlayer(
                        bannedUid,
                        banEndTime,
                        reason,
                        false,
                        args.getSender() != null ? String.valueOf(args.getSender().getUid()) : "Console");
                return "Banned player " + bannedUid + " until " + Utils.formatTimestamp(banEndTime) +
                        (reason != null ? " (" + reason + ")" : "");
            }
            default -> {
                return "Ban sub command not found";
            }
        }
    }
}