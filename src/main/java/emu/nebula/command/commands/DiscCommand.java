package emu.nebula.command.commands;

import emu.nebula.util.Utils;
import emu.nebula.game.character.GameDisc;
import emu.nebula.net.NetMsgId;

import java.util.ArrayList;
import java.util.HashSet;

import emu.nebula.command.Command;
import emu.nebula.command.CommandArgs;
import emu.nebula.command.CommandHandler;

@Command(
        label = "disc", 
        aliases = {"d"}, 
        permission = "player.character", 
        requireTarget = true, 
        desc = "!disc [all | disc ids...] lv(level) a(ascension) c(crescendo level) = Changes the targeted player's discs. Multiple disc ids can be given, and 'all' applies the changes to every owned disc."
)
public class DiscCommand implements CommandHandler {

    @Override
    public String execute(CommandArgs args) {
        // Init
        var player = args.getTarget();
        var discs = new HashSet<GameDisc>();
        
        // Parse args
        for (String arg : args.getList()) {
            // Lowercase
            arg = arg.toLowerCase();
            
            // Handle all discs
            if (arg.equals("all")) {
                discs.addAll(player.getCharacters().getDiscCollection());
                continue;
            }
            
            // Parse disc id
            int charId = Utils.parseSafeInt(arg);
            
            var disc = player.getCharacters().getDiscById(charId);
            if (disc == null) {
                continue;
            }
            
            discs.add(disc);
        }
        
        // Sanity check
        if (discs.isEmpty()) {
            return "No discs selected";
        }
        
        // List of modified characters that we send to the client for updates
        var modified = new ArrayList<GameDisc>();
        
        // Modify characters
        for (var disc : discs) {
            // Apply changes
            boolean changed = args.setProperties(disc);
            
            if (changed) {
                // Save to database
                disc.save();
                
                // Add to modified list
                modified.add(disc);
            }
        }
        
        if (modified.isEmpty()) {
            return "No discs changed";
        }
        
        // Encode and send
        for (var disc : modified) {
            player.addNextPackage(NetMsgId.disc_reset_notify, disc.toProto());
        }
        
        // Return message
        return "Changed " + modified.size() + " discs";
    }
}
