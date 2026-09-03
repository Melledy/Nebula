package emu.nebula.command.commands;

import emu.nebula.Nebula;
import emu.nebula.command.Command;
import emu.nebula.command.CommandArgs;
import emu.nebula.command.CommandHandler;
import emu.nebula.data.GameData;
import emu.nebula.data.resources.InfinityTowerLevelDef;
import emu.nebula.util.Utils;

@Command(label = "arena", aliases = {"setarena"}, permission = "player.arena", requireTarget = false, desc = "!arena [element] [floor] [level] [@id] | !arena clear = Set menace arena progress")
public class ArenaCommand implements CommandHandler {
    private static final String RELOG_HINT = " Restart the game to see the change.";

    // Single source of truth: tower id, canonical element name, aliases
    private record Element(int towerId, String name, String... aliases) {}
    private static final Element[] ELEMENTS = {
        new Element(1, "generic", "none"),
        new Element(2, "ignis", "fire"),
        new Element(3, "ventus", "wind"),
        new Element(4, "lux", "light"),
        new Element(5, "aqua", "water"),
        new Element(6, "terra", "earth"),
        new Element(7, "umbra", "dark"),
    };

    @Override
    public String execute(CommandArgs args) {
        // Get target (null when run from console without @UID, or player offline)
        var target = args.getTarget();

        // @UID given but player not found/offline
        if (target == null && args.getTargetUid() != 0) {
            return "Player @" + args.getTargetUid() + " not found or offline.";
        }

        // No target at all (console without @UID): show usage only
        if (target == null) {
            var sb = new StringBuilder("No @id specified. Run in-game or add @UID to target someone from console.");
            sb.append("\nUsage: !arena [element] [floor] [level] [@id] | !arena clear");
            sb.append("\nElements: generic, ignis, ventus, lux, aqua, terra, umbra");
            sb.append("\nTip: 1 1 clears that element's progress.");
            return sb.toString();
        }

        // No args: show progress + usage
        if (args.size() == 0) {
            var sb = new StringBuilder();
            if (target.getProgress().getInfinityTowerLog().isEmpty()) {
                sb.append("No infinity tower progress for ").append(target.getName()).append(".");
            } else {
                sb.append("Infinity tower progress for ").append(target.getName()).append(":");
                for (var entry : target.getProgress().getInfinityTowerLog().int2IntEntrySet()) {
                    sb.append(" [").append(describeLevel(entry.getIntKey(), entry.getIntValue())).append("]");
                }
            }
            sb.append("\nUsage: !arena [element] [floor] [level] [@id] | !arena clear");
            sb.append("\nElements: generic, ignis, ventus, lux, aqua, terra, umbra");
            sb.append("\nTip: 1 1 clears that element's progress.");
            return sb.toString();
        }

        // Clear all progress
        var progress = target.getProgress();
        if (args.get(0).equalsIgnoreCase("clear")) {
            progress.getInfinityTowerLog().clear();
            progress.save();
            return "Infinity tower progress cleared for " + target.getName() + "." + RELOG_HINT;
        }

        // Parse element -> tower id
        int towerId = parseTowerId(args.get(0));
        if (towerId <= 0) {
            return "Unknown element " + args.get(0) + ". Use generic, ignis, ventus, lux, aqua, terra, umbra.";
        }

        // Parse section (floor) and position (level): both 1-based as displayed in-game.
        // Also accepts 13-8 as one arg.
        String floorArg = args.get(1);
        String levelArg = args.get(2);
        if (floorArg.contains("-")) {
            String[] split = floorArg.split("-", 2);
            floorArg = split[0];
            if (split.length > 1 && !split[1].isEmpty()) {
                levelArg = split[1];
            }
        }
        int section = Utils.parseSafeInt(floorArg);
        int position = Utils.parseSafeInt(levelArg);
        if (section < 1 || section > 20) {
            return "Invalid floor " + floorArg + ". Floor must be 1-20. Usage: !arena [element] [floor] [level] [@id]";
        }
        if (position < 1 || position > 10) {
            return "Invalid level " + levelArg + ". Level must be 1-10. Usage: !arena [element] [floor] [level] [@id]";
        }

        // Find the section (difficulties ordered by id within the tower)
        var diffs = GameData.getInfinityTowerDifficultyDataTable().stream()
                .filter(d -> d.getTowerId() == towerId)
                .sorted((a, b) -> Integer.compare(a.getId(), b.getId()))
                .toList();
        if (section > diffs.size()) {
            return "Tower " + elementName(towerId) + " only has " + diffs.size() + " sections.";
        }
        int difficultyId = diffs.get(section - 1).getId();

        // Find the position within the section (levels ordered by id)
        var levels = GameData.getInfinityTowerLevelDataTable().stream()
                .filter(l -> l.getDifficultyId() == difficultyId)
                .sorted((a, b) -> Integer.compare(a.getId(), b.getId()))
                .toList();
        if (position > levels.size()) {
            return "Section " + section + " only has " + levels.size() + " levels.";
        }
        InfinityTowerLevelDef selected = levels.get(position - 1);

        // The save holds the last BEATEN level, but input is the level to face:
        // back up one. Input 1 1 is the very first level, so it clears the tower instead.
        var towerLevels = GameData.getInfinityTowerLevelDataTable().stream()
                .filter(l -> l.getTowerId() == towerId)
                .sorted((a, b) -> {
                    int cmp = Integer.compare(a.getDifficultyId(), b.getDifficultyId());
                    return cmp != 0 ? cmp : Integer.compare(a.getId(), b.getId());
                })
                .toList();
        int index = towerLevels.indexOf(selected);
        if (index <= 0) {
            progress.getInfinityTowerLog().remove(towerId);
            progress.save();
            return "Cleared " + elementName(towerId) + " progress for " + target.getName() + "." + RELOG_HINT;
        }
        selected = towerLevels.get(index - 1);

        int levelId = selected.getId();
        progress.getInfinityTowerLog().put(towerId, levelId);
        Nebula.getGameDatabase().update(progress, target.getUid(), "infinityTowerLog." + towerId, levelId);

        return "Set " + elementName(towerId) + " " + section + " " + position + " (level " + levelId + ") for " + target.getName() + "." + RELOG_HINT;
    }

    private String elementName(int towerId) {
        if (towerId >= 1 && towerId <= 7) {
            return ELEMENTS[towerId - 1].name();
        }
        return "tower" + towerId;
    }

    // Decode a saved level id back to "element section position" as typed
    private String describeLevel(int towerId, int levelId) {
        var level = GameData.getInfinityTowerLevelDataTable().get(levelId);
        if (level == null) {
            return elementName(towerId) + " ?";
        }
        var diffs = GameData.getInfinityTowerDifficultyDataTable().stream()
                .filter(d -> d.getTowerId() == towerId)
                .sorted((a, b) -> Integer.compare(a.getId(), b.getId()))
                .toList();
        var levels = GameData.getInfinityTowerLevelDataTable().stream()
                .filter(l -> l.getDifficultyId() == level.getDifficultyId())
                .sorted((a, b) -> Integer.compare(a.getId(), b.getId()))
                .toList();
        int section = diffs.indexOf(diffs.stream().filter(d -> d.getId() == level.getDifficultyId()).findFirst().orElse(null)) + 1;
        int position = levels.indexOf(level) + 1;
        return elementName(towerId) + " " + section + " " + position;
    }

    private int parseTowerId(String element) {
        int id = Utils.parseSafeInt(element);
        if (id >= 1 && id <= 7) {
            return id;
        }
        for (var e : ELEMENTS) {
            if (e.name().equalsIgnoreCase(element)) {
                return e.towerId();
            }
            for (var alias : e.aliases()) {
                if (alias.equalsIgnoreCase(element)) {
                    return e.towerId();
                }
            }
        }
        return 0;
    }
}
