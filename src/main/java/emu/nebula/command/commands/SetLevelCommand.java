package emu.nebula.command.commands;

import emu.nebula.command.Command;
import emu.nebula.command.CommandArgs;
import emu.nebula.command.CommandHandler;
import emu.nebula.data.GameData;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.NotifyGm.GmWorldClass;
import emu.nebula.util.Utils;

@Command(label = "setlevel", aliases = {"level", "l"}, permission = "player.level", requireTarget = true, desc = "/level [level]. Set player level")
public class SetLevelCommand implements CommandHandler {

    @Override
    public String execute(CommandArgs args) {
        // Get target
        var target = args.getTarget();
        
        // Parse level
        int level = Utils.parseSafeInt(args.get(0));
        
        // Check to make sure world class data exists for this level
        var data = GameData.getWorldClassDataTable().get(level);
        if (data == null) {
            return "The game does not support level " + level;
        }
        
        target.setLevel(level);
        target.setExp(0);
        
        // Update
        target.addNextPackage(
            NetMsgId.world_class_number_notify,
            GmWorldClass.newInstance()
                .setFinalClass(target.getLevel())
                .setLastExp(target.getExp())
        );
        
        return "Level set to " + level;
    }
}
