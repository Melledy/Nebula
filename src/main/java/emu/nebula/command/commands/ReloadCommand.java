package emu.nebula.command.commands;

import emu.nebula.Nebula;
import emu.nebula.command.Command;
import emu.nebula.command.CommandArgs;
import emu.nebula.command.CommandHandler;

@Command(label = "reload", permission = "admin.reload", desc = "!reload = Reloads the server config and the game patch list.")
public class ReloadCommand implements CommandHandler {

    @Override
    public String execute(CommandArgs args) {
        // Reload config first
        Nebula.loadConfig();
        
        // Reload patch list if the server is running
        if (Nebula.getHttpServer() != null) {
            Nebula.getHttpServer().loadPatchList();
        }
        
        // Result message
        return "Reloaded the server config";
    }

}
