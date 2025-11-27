package emu.nebula.command.commands;

import emu.nebula.Nebula;
import emu.nebula.command.Command;
import emu.nebula.command.CommandArgs;
import emu.nebula.command.CommandHandler;

import java.util.Random;

@Command(label = "remote", permission = "player.remote", requireTarget = true, desc = "/remote. Send remote to web remote")
public class RemoteKeyCommand implements CommandHandler {
    private final String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    @Override
    public String execute(CommandArgs args) {
        if (Nebula.getConfig().getRemoteCommand().useRemoteServices) {
            StringBuilder sb = new StringBuilder();
            Random random = new Random();

            for (int i = 0; i < 8; i++) {
                int index = random.nextInt(characters.length());
                sb.append(characters.charAt(index));
            }

            args.getTarget().setRemoteToken(sb.toString());
            return "Key Generated: " + sb.toString();
        } else {
            args.getTarget().setRemoteToken(null);
            return "Remote Command Disabled on Server";
        }
    }
}
