package emu.nebula.command.commands;

import emu.nebula.util.Utils;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import emu.nebula.game.character.CharacterGem;
import emu.nebula.game.character.GameCharacter;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.PubilcGm.Chars;

import emu.nebula.command.Command;
import emu.nebula.command.CommandArgs;
import emu.nebula.command.CommandHandler;
import emu.nebula.data.GameData;

@Command(
        label = "emblem", 
        aliases = {"e", "gem"}, 
        permission = "player.emblem", 
        requireTarget = true,
        desc = "!emblem [character id] [emblem slot] [attribute value id 1] [attribute value id 2] [attribute value id 3] [attribute value id 4] = Sets the four attribute values of an emblem in the targeted player's character emblem slot, creating or replacing the gem as needed."
)
public class EmblemCommand implements CommandHandler {

    @Override
    public String execute(CommandArgs args) {
        // Init
        var player = args.getTarget();
        
        // Sanity check
        if (args.getList().size() < 6) {
            return "Error: Not enough args";
        }
        
        // Parse args
        var charId = Utils.parseSafeInt(args.getList().get(0));
        var slotId = Utils.parseSafeInt(args.getList().get(1));
        var attributes = new IntArrayList();
        
        for (int i = 2; i < 6; i++) {
            int attr = Utils.parseSafeInt(args.getList().get(i));
            var data = GameData.getCharGemAttrValueDataTable().get(attr);
            if (data == null) {
                return "Error: Invalid attribute value id in position " + (i - 2);
            }
            
            attributes.add(attr);
        }
        
        // Get character
        GameCharacter character = player.getCharacters().getCharacterById(charId);
        
        if (character == null) {
            return "Error: No trekker selected";
        }
        
        // Get gem slot
        var slot = character.getGemSlot(slotId);
        
        if (slot == null) {
            return "Error: Invalid slot selected";
        }
        
        // Get gem
        var preset = character.getCurrentGemPreset();
        var gem = slot.getGem(preset.getGemIndex(slotId));
        
        if (gem == null) {
            if (slot.isFull()) {
                // Equip the first gem we can find
                preset.setGemIndex(slotId, 0);
                gem = slot.getGem(preset.getGemIndex(slotId));
            } else {
                // Create a new gem and equip it
                gem = new CharacterGem(attributes);
                
                // Add gem to slot
                slot.getGems().add(gem);
                
                // Equip
                preset.setGemIndex(slotId, slot.getGems().size() - 1);
            }
            
            // Make sure gem exists
            if (gem == null) {
                return "An unknown error has occured";
            }
        }
        
        // Replace gem
        gem.setAttributes(attributes);
        
        // Save
        character.save();
        
        // Encode and send
        var proto = Chars.newInstance()
                .addList(character.toProto());
        
        player.addNextPackage(NetMsgId.chars_final_notify, proto);
        
        // Result
        return "Updated emblem for character";
    }
}
