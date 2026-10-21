package emu.nebula.command.commands;

import emu.nebula.command.Command;
import emu.nebula.command.CommandArgs;
import emu.nebula.command.CommandHandler;
import emu.nebula.data.GameData;
import emu.nebula.data.resources.ItemDef;
import emu.nebula.game.inventory.ItemParamMap;
import emu.nebula.game.inventory.ItemType;
import emu.nebula.game.player.PlayerChangeInfo;
import emu.nebula.net.NetMsgId;
import emu.nebula.util.Utils;

import java.util.HashSet;

@Command(
        label = "clean",
        aliases = {"cl", "clear"},
        permission = "player.inventory",
        requireTarget = true,
        desc = "!clean [all | item ids...] [items | resources] = Removes items and/or resources from the targeted player. Use 'all' to remove everything, or add 'items' or 'resources' to limit the cleanup to one type."
)
public class CleanCommand implements CommandHandler {

    @Override
    public String execute(CommandArgs args) {
        var player = args.getTarget();
        var inv = player.getInventory();

        boolean doItems = true;
        boolean doResources = true;
        boolean all = false;

        var ids = new HashSet<Integer>();

        for (String arg : args.getList()) {
            arg = arg.toLowerCase();

            switch (arg) {
                case "items" -> {
                    doResources = false;
                    continue;
                }
                case "resources", "res" -> {
                    doItems = false;
                    continue;
                }
                case "all" -> {
                    all = true;
                    continue;
                }
            }

            int id = Utils.parseSafeInt(arg);
            if (id > 0) {
                ids.add(id);
            }
        }

        var change = new PlayerChangeInfo();
        var removeMap = new ItemParamMap();

        if (all) {
            if (doItems) {
                for (var entry : inv.getItems().int2IntEntrySet()) {
                    removeMap.add(entry.getIntKey(), entry.getIntValue());
                }
            }

            if (doResources) {
                for (var entry : inv.getResources().int2IntEntrySet()) {
                    removeMap.add(entry.getIntKey(), entry.getIntValue());
                }
            }
        } else {
            for (int id : ids) {
                ItemDef data = GameData.getItemDataTable().get(id);
                if (data == null) continue;
                
                ItemType type = data.getItemType();

                switch (type) {
                    case Res -> {
                        if (doResources) {
                            int count = inv.getResourceCount(id);
                            if (count > 0) removeMap.add(id, count);
                        }
                    }
                    case Item -> {
                        if (doItems) {
                            int count = inv.getItemCount(id);
                            if (count > 0) removeMap.add(id, count);
                        }
                    }
                    case Disc, Char, CharacterSkin, Title, Honor -> {
                        
                    }
                    default -> {
                        if (doItems) {
                            int count = inv.getItemCount(id);
                            if (count > 0) {
                                removeMap.add(id, count);
                                break;
                            }
                        }
                        if (doResources) {
                            int count = inv.getResourceCount(id);
                            if (count > 0) removeMap.add(id, count);
                        }
                    }
                }
            }
        }

        if (!removeMap.isEmpty()) {
            change = inv.removeItems(removeMap, change);
        }

        if (change.isEmpty()) {
            return "No items/resources removed";
        }

        player.addNextPackage(NetMsgId.items_change_notify, change.toProto());
        
        return "Inventory cleaned";
    }
}
