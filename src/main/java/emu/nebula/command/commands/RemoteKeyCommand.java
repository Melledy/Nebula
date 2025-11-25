package emu.nebula.command.commands;

import emu.nebula.Nebula;
import emu.nebula.command.Command;
import emu.nebula.command.CommandArgs;
import emu.nebula.command.CommandHandler;

import java.util.Random;

@Command(label = "remote", permission = "player.remote", requireTarget = true, desc = "/remote. Send remote to web remote")
public class RemoteKeyCommand implements CommandHandler {

    private static String lastMessage;

    public static String getLastMessage() {
        return lastMessage;
    }

    @Override
    public void execute(CommandArgs args) {
        if (Nebula.getConfig().getRemoteCommand().useRemoteServices) {
            String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
            StringBuilder sb = new StringBuilder();
            Random random = new Random();

            for (int i = 0; i < 8; i++) {
                int index = random.nextInt(characters.length());
                sb.append(characters.charAt(index));
            }
            args.getTarget().setPlayerRemoteToken(sb.toString());
            args.getTarget().save();
            String textsend = "Key Generated: " + sb.toString();
            lastMessage = textsend;
            args.sendMessage(textsend);
            return;
        }
        String textsend = "RemoteCommand Disabled on Server";
        args.getTarget().setPlayerRemoteToken(null);
        args.getTarget().save();
        lastMessage = textsend;
        args.sendMessage(textsend);

    }
}
