package emu.nebula.command.commands;

import emu.nebula.command.Command;
import emu.nebula.command.CommandArgs;
import emu.nebula.command.CommandHandler;
import emu.nebula.data.GameData;
import emu.nebula.data.resources.ItemDef;
import emu.nebula.game.inventory.GameItem;
import emu.nebula.game.inventory.GameResource;
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
        desc = "!clean [all | {id} ...] [items|resources]. Removes items/resources from the targeted player."
)
public class CleanCommand implements CommandHandler {

    @Override
    public void execute(CommandArgs args) {
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
                for (GameItem item : inv.getItems().values()) {
                    removeMap.add(item.getItemId(), item.getCount());
                }
            }

            if (doResources) {
                for (GameResource res : inv.getResources().values()) {
                    removeMap.add(res.getResourceId(), res.getCount());
                }
            }
        } else {
            for (int id : ids) {
                ItemDef data = GameData.getItemDataTable().get(id);

                if (data != null) {
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
                } else {
                    args.sendMessage("Error: Invalid item id " + id);
                }
            }
        }

        if (!removeMap.isEmpty()) {
            change = inv.removeItems(removeMap, change);
        }

        if (change.isEmpty()) {
            args.sendMessage("No items/resources removed");
            return;
        }

        player.addNextPackage(NetMsgId.items_change_notify, change.toProto());
        args.sendMessage("Inventory cleaned");
    }
}
