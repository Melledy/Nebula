package emu.nebula.command.commands;

import java.util.ArrayList;
import java.util.List;

import emu.nebula.command.Command;
import emu.nebula.command.CommandArgs;
import emu.nebula.command.CommandHandler;
import emu.nebula.data.GameData;
import emu.nebula.game.character.GameCharacter;
import emu.nebula.game.character.GameDisc;
import emu.nebula.game.player.Player;
import emu.nebula.game.tower.StarTowerBuild;
import emu.nebula.net.NetMsgId;
import emu.nebula.util.Utils;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import lombok.Getter;

@Command(
    label = "build", 
    aliases = {"b", "record", "r"}, 
    permission = "player.build", 
    requireTarget = true,
    desc = "!build [character ids...] [disc ids...] [potential ids...] [sub-note skill ids...] = Creates a record for the targeted player. Requires exactly 3 characters and 3-6 discs. Potential levels can be set with id:level, and a preset code can be used instead of item ids."
)
public class BuildCommand implements CommandHandler {

    @Override
    public String execute(CommandArgs args) {
        // Create record
        var target = args.getTarget();
        var builder = new StarTowerBuildData(target);
        
        // Parse items
        for (String arg : args.getList()) {
            int id = Utils.parseSafeInt(arg);
            int count = 1;
            
            if (id != 0) {
                builder.parseItem(id, count);
            } else if (!Utils.isNumeric(arg)) {
                // Might be a preset code
                var items = args.parsePresetCode(arg);
                if (items != null) {
                    builder.parseItems(items);
                }
            }
        }
        
        if (args.getMap() != null) {
            builder.parseItems(args.getMap());
        }
        
        // Check if build is valid
        if (builder.getCharacters().size() != 3) {
            return "Record must have 3 different characters";
        }
        
        if (builder.getDiscs().size() < 3 || builder.getDiscs().size() > 6) {
            return "Record must have 3-6 different discs";
        }
        
        // Create record
        var build = builder.toBuild();
        
        // Add to star tower manager
        target.getStarTowerManager().addBuild(build);
        
        // Send package to player
        target.addNextPackage(NetMsgId.st_import_build_notify, build.toProto());
            
        // Send result to player
        String result = "Created record for " + target.getName();
        
        if (args.getSender() == null) {
            result += " (This command may take time to update on the client)";
        }
        
        return result;
    }

    @Getter
    public static class StarTowerBuildData {
        private Player player;
        private StarTowerBuild build;
        private List<GameCharacter> characters;
        private List<GameDisc> discs;
        
        public StarTowerBuildData(Player player) {
            this.player = player;
            this.build = new StarTowerBuild(player);
            this.characters = new ArrayList<>();
            this.discs = new ArrayList<>();
        }
        
        public void parseItem(int id, int count) {
            // Get item data
            var itemData = GameData.getItemDataTable().get(id);
            if (itemData == null) {
                return;
            }
            
            // Clamp
            count = Math.max(count, 1);
            
            // Parse by item id
            switch (itemData.getItemSubType()) {
                case Char -> {
                    var character = this.getPlayer().getCharacters().getCharacterById(id);
                    if (character == null || !character.getData().isAvailable()) {
                        break;
                    }
                    
                    this.addCharacter(character);
                }
                case Disc -> {
                    var disc = this.getPlayer().getCharacters().getDiscById(id);
                    if (disc == null || !disc.getData().isAvailable()) {
                        break;
                    }
                    
                    this.addDisc(disc);
                }
                case Potential, SpecificPotential -> {
                    var potentialData = GameData.getPotentialDataTable().get(id);
                    if (potentialData == null) break;
                    
                    int level = Math.min(count, potentialData.getMaxLevel());
                    this.getBuild().getPotentials().add(id, level);
                }
                case SubNoteSkill -> {
                    this.getBuild().getSubNoteSkills().add(id, count);
                }
                default -> {
                    // Ignored
                }
            }
        }
        
        public void parseItems(Int2IntMap items) {
            for (var entry : items.int2IntEntrySet()) {
                int id = entry.getIntKey();
                int count = entry.getIntValue();
                
                this.parseItem(id, count);
            }
        }
        
        public void addCharacter(GameCharacter character) {
            if (this.characters.contains(character)) {
                return;
            }
            
            this.characters.add(character);
        }
        
        public void addDisc(GameDisc disc) {
            if (this.discs.contains(disc)) {
                return;
            }
            
            this.discs.add(disc);
        }
        
        public StarTowerBuild toBuild() {
            // Set characters and discs
            build.setChars(this.getCharacters());
            build.setDiscs(this.getDiscs());
            
            // Clear character potential cache
            build.getCharPots().clear();
            
            for (int charId : build.getCharIds()) {
                build.getCharPots().put(charId, 0);
            }
            
            // Add potentials to character potential cache
            var it = build.getPotentials().iterator();
            while (it.hasNext()) {
                var potential = it.next();
                
                var data = GameData.getPotentialDataTable().get(potential.getIntKey());
                if (data == null || !build.getCharPots().containsKey(data.getCharId())) {
                    it.remove();
                    continue;
                }
                
                build.getCharPots().add(data.getCharId(), 1);
            }
            
            // Reset secondary skills
            build.refreshSecondarySkills();
            
            // Calculate score
            build.calculateScore();
            
            // Return build
            return build;
        }
    }
}
