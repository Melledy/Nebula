package emu.nebula.command.commands;

import java.util.Set;

import emu.nebula.GameConstants;
import emu.nebula.command.Command;
import emu.nebula.command.CommandArgs;
import emu.nebula.command.CommandHandler;
import emu.nebula.data.GameData;
import emu.nebula.data.resources.ItemDef;
import emu.nebula.game.inventory.ItemParamMap;
import emu.nebula.game.inventory.ItemSubType;
import emu.nebula.game.player.Player;
import emu.nebula.game.player.PlayerChangeInfo;
import emu.nebula.net.NetMsgId;

@Command(
        label = "giveall", 
        aliases = {"ga"}, 
        permission = "player.give", 
        requireTarget = true, 
        desc = "!ga [characters | discs | materials] lv(level) t(talent/crescendo level) s(skill level). Gives the targeted player items."
)
public class GiveAllCommand implements CommandHandler {
    private static Set<ItemSubType> MATERIAL_ITEM_SUBTYPES = Set.of(
        ItemSubType.DiscStrengthen,         // Disc exp
        ItemSubType.DiscPromote,            // Disc tier up
        ItemSubType.SkillStrengthen,        // Skill upgrade
        ItemSubType.CharacterLimitBreak,    // Character tier up
        ItemSubType.Equipment               // Emblem crafting
    );

    @Override
    public String execute(CommandArgs args) {
        Player target = args.getTarget();
        String type = args.get(0).toLowerCase();
        
        var change = new PlayerChangeInfo();
        var message = new StringBuilder();

        switch (type) {
            case "m", "materials", "mats" -> {
                // Create items map
                var items = new ItemParamMap();
                
                // Check sub type
                for (ItemDef data : GameData.getItemDataTable()) {
                    if (data.getItemSubType() == null || !MATERIAL_ITEM_SUBTYPES.contains(data.getItemSubType())) {
                        continue;
                    }
                    
                    items.add(data.getId(), 10_000);
                }
                
                // Character exp, not sure why this doesnt have an unique sub type so we have to hard code it
                items.add(30001, 10_000);
                items.add(30002, 10_000);
                items.add(30003, 10_000);
                items.add(30004, 10_000);

                // Gold
                items.add(GameConstants.GOLD_ITEM_ID, 50_000_000);

                // Add to target's inventory
                target.getInventory().addItems(items, change);

                // Send message
                message.append("Giving " + target.getName() + " " + items.size() + " items.\n");
            }
            case "d", "discs" -> {
                // Get all discs
                for (var data : GameData.getDiscDataTable()) {
                    // Skip unavailable discs
                    if (!data.isAvailable() || !data.isVisible()) {
                        continue;
                    }
                    
                    // Check if we have the disc already
                    if (target.getCharacters().hasDisc(data.getId())) {
                        continue;
                    }
                    
                    // Add to player
                    var disc = target.getCharacters().addDisc(data.getId());
                    
                    // Set properties
                    boolean shouldSave = args.setProperties(disc);
                    
                    if (shouldSave) {
                        disc.save();
                    }
                    
                    // Add to change info
                    change.add(disc.toProto());
                }

                // Send message
                message.append("Giving " + target.getName() + " all discs.\n");
            }
            case "c", "characters", "trekkers", "t" -> {
                // Get all characters
                for (var data : GameData.getCharacterDataTable()) {
                    // Skip unavailable characters
                    if (!data.isAvailable() || !data.isVisible()) {
                        continue;
                    }
                    
                    // Check if we have the character already
                    if (target.getCharacters().hasCharacter(data.getId())) {
                        continue;
                    }
                    
                    // Add to player
                    var character = target.getCharacters().addCharacter(data.getId());
                    
                    // Set properties
                    boolean shouldSave = args.setProperties(character);
                    
                    if (shouldSave) {
                        character.save();
                    }
                    
                    // Add to change info
                    change.add(character.toProto());
                }

                // Send message
                message.append("Giving " + target.getName() + " all characters.\n");
            }
            default -> {
                // Ignored
            }
        }
        
        if (change.isEmpty()) {
            return "No items given to the player";
        }
        
        // Encode and send
        target.addNextPackage(NetMsgId.items_change_notify, change.toProto());
        
        // Complete
        return message.toString();
    }

}
