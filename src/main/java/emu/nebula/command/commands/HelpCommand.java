package emu.nebula.command.commands;

import emu.nebula.Nebula;
import emu.nebula.command.Command;
import emu.nebula.command.CommandArgs;
import emu.nebula.command.CommandHandler;

@Command(label = "help", permission = "player.help", desc = "!help = Displays a list of every command available to you. (Very spammy in-game)")
public class HelpCommand implements CommandHandler {

    @Override
    public String execute(CommandArgs args) {
        var message = new StringBuilder();
        
        message.append("Displaying list of commands:\n");

        // Sort command names
        var labels = Nebula.getCommandManager().getLabels().keySet().stream().sorted().toList();
        for (var label : labels) {
            // Get command
            Command command = Nebula.getCommandManager().getLabels().get(label).getClass().getAnnotation(Command.class);
            if (command == null) continue;

            // Only send command description if the sender has permission to use the command
            if (Nebula.getCommandManager().checkPermission(args.getSender(), command)) {
                message.append(command.desc());
                message.append("\n");
            }
        }
        
        return message.toString();
    }
}