package emu.nebula.command.commands;

import emu.nebula.util.Utils;
import emu.nebula.game.character.GameCharacter;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.PubilcGm.Chars;

import java.util.ArrayList;
import java.util.HashSet;

import emu.nebula.command.Command;
import emu.nebula.command.CommandArgs;
import emu.nebula.command.CommandHandler;

@Command(
        label = "character", 
        aliases = {"c", "char"}, 
        permission = "player.character", 
        requireTarget = true,
        desc = "!c [all | {characterId}] lv(level) a(ascension) s(skill level) t(talent level) f(affinity level)"
)
public class CharacterCommand implements CommandHandler {

    @Override
    public void execute(CommandArgs args) {
        // Init
        var player = args.getTarget();
        var characters = new HashSet<GameCharacter>();
        
        // Parse args
        for (String arg : args.getList()) {
            // Lowercase
            arg = arg.toLowerCase();
            
            // Handle all characters
            if (arg.equals("all")) {
                characters.addAll(player.getCharacters().getCharacterCollection());
                continue;
            }
            
            // Parse char id
            int charId = Utils.parseSafeInt(arg);
            
            var character = player.getCharacters().getCharacterById(charId);
            if (character == null) {
                continue;
            }
            
            characters.add(character);
        }
        
        // Sanity check
        if (characters.isEmpty()) {
            args.sendMessage("Error: No characters selected");
            return;
        }
        
        // List of modified characters that we send to the client for updates
        var modified = new ArrayList<GameCharacter>();
        
        // Modify characters
        for (var character : characters) {
            // Apply changes
            boolean changed = args.setProperties(character);
            
            if (changed) {
                // Save to database
                character.save();
                
                // Add to modified list
                modified.add(character);
            }
        }
        
        if (modified.isEmpty()) {
            args.sendMessage("No changes applied");
            return;
        }
        
        // Encode and send
        var proto = Chars.newInstance();
        
        for (var character : modified) {
            proto.addList(character.toProto());
        }
        
        player.addNextPackage(NetMsgId.chars_final_notify, proto);
        args.sendMessage("Updated " + modified.size() + " character(s)");
    }
}
