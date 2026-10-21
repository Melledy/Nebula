package emu.nebula.command.commands;

import emu.nebula.util.Utils;
import emu.nebula.data.GameData;
import emu.nebula.game.mail.GameMail;
import emu.nebula.command.Command;
import emu.nebula.command.CommandArgs;
import emu.nebula.command.CommandHandler;

@Command(
        label = "give", 
        aliases = {"g", "item"}, 
        permission = "player.give", 
        requireTarget = true, 
        desc = "!give [item ids...] x(amount) = Sends the targeted player the specified items through the mail. Defaults to 1 of each item."
)
public class GiveCommand implements CommandHandler {

    @Override
    public String execute(CommandArgs args) {
        // Setup mail
        var mail = new GameMail("System", "Give Command Result", "");
        
        // Get amount to give
        int amount = Math.max(args.getAmount(), 1);
        
        // Parse items
        for (String arg : args.getList()) {
            // Parse item id
            int itemId = Utils.parseSafeInt(arg);
            
            var itemData = GameData.getItemDataTable().get(itemId);
            if (itemData == null) {
                continue;
            }
            
            // Add
            mail.addAttachment(itemId, amount);
        }
        
        // Add mail
        args.getTarget().getMailbox().sendMail(mail);
        
        //
        return "Give command success, check your mail";
    }
}
