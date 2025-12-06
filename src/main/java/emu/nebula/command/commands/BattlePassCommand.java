package emu.nebula.command.commands;

import emu.nebula.command.Command;
import emu.nebula.command.CommandArgs;
import emu.nebula.command.CommandHandler;
import emu.nebula.net.NetMsgId;

@Command(label = "battlepass", aliases = {"bp"}, permission = "player.battlepass", desc = "/battlepass [free | premium] lv(level). mMdifies your battle pass")
public class BattlePassCommand implements CommandHandler {

    @Override
    public String execute(CommandArgs args) {
        // Get target
        var target = args.getTarget();
        var battlepass = target.getBattlePassManager().getBattlePass();
        boolean changed = false;
        
        // Check if we are changing premium status
        int mode = -1;
        
        for (var arg : args.getList()) {
            if (arg.equalsIgnoreCase("free")) {
                mode = 0;
            } else if (arg.equalsIgnoreCase("premium")) {
                mode = 2;
            }
        }
        
        if (mode >= 0 && battlepass.getMode() != mode) {
            battlepass.setMode(mode);
            changed = true;
        }
        
        // Set level
        int level = Math.min(args.getLevel(), 50);
        
        if (level >= 0 && battlepass.getLevel() != level) {
            battlepass.setLevel(level);
            changed = true;
        }
        
        // Check if we have made any changes
        if (changed) {
            // Save battle pass to the database
            battlepass.save();
            
            // Send package to notify the client that the battle pass needs updating
            target.addNextPackage(
                NetMsgId.battle_pass_info_succeed_ack, 
                battlepass.toProto()
            );
            
            // Success message
            return "Changed the battle pass successfully.";
        }
        
        // Result message
        return "No changes were made to the battle pass.";
    }

}
