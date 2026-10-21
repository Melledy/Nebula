package emu.nebula.command.commands;

import java.util.ArrayList;
import java.util.Collections;

import emu.nebula.GameConstants;
import emu.nebula.command.Command;
import emu.nebula.command.CommandArgs;
import emu.nebula.command.CommandHandler;
import emu.nebula.command.commands.BuildCommand.StarTowerBuildData;
import emu.nebula.data.GameData;
import emu.nebula.game.character.GameCharacter;
import emu.nebula.game.character.GameDisc;
import emu.nebula.game.inventory.ItemParamMap;
import emu.nebula.net.NetMsgId;
import emu.nebula.util.Utils;
import it.unimi.dsi.fastutil.ints.IntArrayList;

@Command(
    label = "autobuild", 
    aliases = {"ab"}, 
    permission = "player.build", 
    requireTarget = true,
    desc = "!autobuild [character/disc/potential/sub-note skill ids...] lv(record level) s(record score) [-nn] [-np] = Generates a record for the targeted player, auto-filling random characters, discs, potentials, and sub-note skills to reach the target record level (default 40) or score. -nn skips sub-note skills and -np skips potentials."
)
public class AutoBuildCommand implements CommandHandler {
    private static final int[] COMMON_SUB_NOTE_SKILLS = new int[] {
        90011, 90012, 90013, 90014, 90015, 90016, 90017
    };
    
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
        
        // Remove extra characters/discs
        while (builder.getCharacters().size() > 3) {
            builder.getCharacters().removeLast();
        }
        
        while (builder.getDiscs().size() > 6) {
            builder.getDiscs().removeLast();
        }
        
        // Add random characters/discs
        if (builder.getCharacters().size() < 3) {
            int count = 3 - builder.getCharacters().size();
            for (int i = 0; i < count; i++) {
                this.pickRandomCharacter(builder);
            }
            
            if (builder.getCharacters().size() < 3) {
                return "Error: Not enough trekkers in the record";
            }
        }
        
        if (builder.getDiscs().size() < 6) {
            int count = 6 - builder.getDiscs().size();
            for (int i = 0; i < count; i++) {
                this.pickRandomDisc(builder);
            }
        }
        
        // Get target level/score
        int targetLevel = 0;
        int targetScore = 0;
        
        if (args.getSkill() < 0) {
            // Default target level is 40
            targetLevel = 40;
        } else {
            targetScore = args.getSkill();
        }
        
        if (args.getLevel() > 0) {
            // Target level overrides target score.
            targetLevel = args.getLevel();
        }
        
        // Get target score from record level
        if (targetLevel > 0) {
            var data = GameData.getStarTowerBuildRankDataTable().get(targetLevel);
            if (data != null) {
                targetScore = data.getMinGrade();
            }
        }
        
        // Calcluate score
        builder.toBuild();
        
        // Pick random potentials and sub notes
        boolean shouldGenerateNotes = !args.hasFlag("-nn") && !args.hasFlag("-nonotes");
        boolean shouldGeneratePotentials = !args.hasFlag("-np") && !args.hasFlag("-nopotentials") ;
        
        if (shouldGenerateNotes) {
            int score = Math.max(targetScore - builder.getBuild().getScore(), 0);
            
            if (shouldGeneratePotentials) {
                score = (int) (score * .4D);
            }
            
            this.generateSubNotes(builder, score);
        }
        
        if (shouldGeneratePotentials) {
            int score = Math.max(targetScore - builder.getBuild().getScore(), 0);
            this.generatePotentials(builder, score);
        }
        
        // Create record
        var build = builder.toBuild();
        
        // Add to star tower manager
        target.getStarTowerManager().addBuild(build);
        
        // Send package to player
        target.addNextPackage(NetMsgId.st_import_build_notify, build.toProto());
            
        // Send result to player
        String result = "Generated record for " + target.getName();
        
        if (args.getSender() == null) {
            result += " (This command may take time to update on the client)";
        }
        
        return result;
    }
    
    private void pickRandomCharacter(StarTowerBuildData builder) {
        // Random list
        var list = new ArrayList<GameCharacter>();
        
        // Create list of possible characters to add
        var characters =  builder.getPlayer().getCharacters().getCharacterCollection()
                .stream()
                .filter(c -> !builder.getCharacters().contains(c))
                .toList();
        
        // Check if record is empty
        if (builder.getCharacters().isEmpty()) {
            // Get random vanguard trekker
            for (var character : characters) {
                if (character.getData().getDes().getTag().contains(GameConstants.CHARACTER_TAG_VANGUARD)) {
                    list.add(character);
                }
            }
            
            // Add any trekker if we dont have any vanguard trekkers
            if (list.isEmpty()) {
                list.addAll(characters); 
            }
        } else {
            // Get element of main trekker
            var main = builder.getCharacters().get(0);
            var element = main.getElementType();
            
            // Get trekkers of the same element
            for (var character : characters) {
                if (character.getData().getElementType() == element) {
                    list.add(character);
                }
            }
            
            // Add any trekker if we dont have any trekkers of the same element
            if (list.isEmpty()) {
                list.addAll(characters); 
            }
            
            // Shuffle list to make it random
            Collections.shuffle(list);
            
            // Add first support trekker we find
            for (var character : list) {
                if (character.getData().getDes().getTag().contains(GameConstants.CHARACTER_TAG_SUPPORT)) {
                    builder.getCharacters().add(character);
                    return;
                }
            }
            
            // If we have no support trekkers, then we look for versatile trekkers
            for (var character : list) {
                if (character.getData().getDes().getTag().contains(GameConstants.CHARACTER_TAG_VERSATILE)) {
                    builder.getCharacters().add(character);
                    return;
                }
            }
        }
        
        // If we have no versatile or support trekkers, add a random trekker from the remaining list
        if (list.size() > 0) {
            builder.getCharacters().add(Utils.randomElement(list));
        }
    }
    
    private void pickRandomDisc(StarTowerBuildData builder) {
        // Get element of main trekker
        var main = builder.getCharacters().get(0);
        var element = main.getElementType();
        
        // Random list
        var list = new ArrayList<GameDisc>();
        
        // Create list of possible discs to add
        var discs = builder.getPlayer().getCharacters().getDiscCollection()
                .stream()
                .filter(d -> !builder.getDiscs().contains(d))
                .toList();
        
        // Get discs of the same element
        for (var disc : discs) {
            if (disc.getData().getElementType() == element) {
                list.add(disc);
            }
        }
        
        if (list.isEmpty()) {
            list.addAll(discs);
        }
        
        // End early if list is still empty
        if (list.isEmpty()) {
            return;
        }
        
        // Shuffle list to make it random
        Collections.shuffle(list);
        
        // Find random disc
        for (var disc : list) {
            var item = GameData.getItemDataTable().get(disc.getDiscId());
            if (item.getRarity() == 1) {
                builder.getDiscs().add(disc);
                return;
            }
        }
        
        for (var disc : list) {
            var item = GameData.getItemDataTable().get(disc.getDiscId());
            if (item.getRarity() == 2) {
                builder.getDiscs().add(disc);
                return;
            }
        }
        
        // Just add the first disc that we can find
        builder.getDiscs().add(list.get(0));
    }
        
    private void generateSubNotes(StarTowerBuildData builder, int subNoteScore) {
        // Get possible drops
        var drops = new IntArrayList();
        
        for (var character : builder.getCharacters()) {
            var element = character.getData().getElementType();
            
            if (element.getSubNoteSkillItemId() == 0) {
                continue;
            }
            
            if (!drops.contains(element.getSubNoteSkillItemId())) {
                drops.add(element.getSubNoteSkillItemId());
            }
        }
        
        for (var disc : builder.getDiscs()) {
            var element = disc.getData().getElementType();
            
            if (element.getSubNoteSkillItemId() == 0) {
                continue;
            }
            
            if (!drops.contains(element.getSubNoteSkillItemId())) {
                drops.add(element.getSubNoteSkillItemId());
            }
        }
        
        for (int id : COMMON_SUB_NOTE_SKILLS) {
            drops.add(id);
        }
        
        // Randomize sub note ids
        Collections.shuffle(drops);
        
        // Distribute sub notes randomly
        int amount = (int) Math.ceil(subNoteScore / 15D);
        int totalSubNotes = amount;
        
        // Allocate budget for each sub note
        var budget = new ItemParamMap();
        double totalValue = 0;
        
        for (int subNote : drops) {
            int value = Utils.randomRange(1, 10);
            budget.add(subNote, value);
            totalValue += value;
        }
        
        // Add random sub notes
        for (int subNote : drops) {
            // Get budgeted value
            int value = budget.get(subNote);
            int count = (int) Math.ceil((value / totalValue) * totalSubNotes);
            
            // Get current sub notes
            int cur = builder.getBuild().getSubNoteSkills().get(subNote);
            int max = Math.max(99 - cur, 0);
            
            // Clamp
            count = Math.min(Math.min(count, amount), max);
            amount -= count;
            
            // Add sub notes
            builder.getBuild().getSubNoteSkills().add(subNote, count);
        }
        
        // Add leftover sub notes
        if (amount > 0) {
            // Randomize again
            Collections.shuffle(drops);
            
            // Add to first sub note that has less than 99
            for (int subNote : drops) {
                // End if we have no more sub notes to give
                if (amount <= 0) {
                    break;
                }
                
                // Get current sub notes
                int cur = builder.getBuild().getSubNoteSkills().get(subNote);
                if (cur >= 99) {
                    continue;
                }
                
                // Add
                int count = Math.min(99 - cur, amount);
                amount -= count;
                
                builder.getBuild().getSubNoteSkills().add(subNote, count);
            }
        }
        
        // Calcluate score
        builder.toBuild();
    }
    
    private void generatePotentials(StarTowerBuildData builder, int potentialScore) {
        // Init weighted list of characters
        var characters = new ArrayList<GameCharacter>();
        
        characters.add(builder.getCharacters().get(0));
        characters.add(builder.getCharacters().get(0)); // Main character gets an extra chance to get more potentials
        characters.add(builder.getCharacters().get(1));
        characters.add(builder.getCharacters().get(2));
        
        Collections.shuffle(characters);
        
        // Get current amount of special potentials
        var specialCounter = new ItemParamMap();
        
        for (var entry : builder.getBuild().getPotentials()) {
            var potential = GameData.getPotentialDataTable().get(entry.getIntKey());
            if (potential == null) continue;
            
            if (potential.isSpecial()) {
                specialCounter.add(potential.getCharId(), 1);
            }
        }
        
        // Cache main trekker
        var main = builder.getCharacters().get(0);
        
        // Get random potentials
        while (potentialScore > 0 && !characters.isEmpty()) {
            // Get random character
            var character = Utils.randomElement(characters);
            
            // Get character potential data
            var data = GameData.getCharPotentialDataTable().get(character.getCharId());
            
            if (data == null) {
                break;
            }
            
            // Check if we should give a special potential
            int sp = specialCounter.get(character.getCharId());
            boolean special = false;
            
            if (sp < 2 && potentialScore >= 180) {
                special = Utils.randomChance(.25);
            }
            
            if (special) {
                specialCounter.add(character.getCharId(), 1);
            }
            
            // Get possible potential list
            var list = data.getPotentialList(main == character, special);
            
            // Remove potentials we already have maxed out
            var potentials = new IntArrayList();
            
            for (int id : list) {
                // Get potential data
                var potential = GameData.getPotentialDataTable().get(id);
                if (potential == null) continue;
                
                // Filter out max level ones
                int curLevel = builder.getBuild().getPotentials().get(id);
                int maxLevel = potential.getMaxLevel();
                
                if (curLevel >= maxLevel) {
                    continue;
                }
                
                // Add
                potentials.add(id);
            }
            
            // Remove character if we dont have any possible potentials for it
            if (potentials.isEmpty()) {
                characters.removeIf(c -> c == character);
                continue;
            }
            
            // Get random potential
            int id = Utils.randomElement(potentials);
            var potential = GameData.getPotentialDataTable().get(id);
            
            // Add
            builder.getBuild().getPotentials().add(id, 1);
            
            // Decrement score
            potentialScore -= potential.getBuildScore(1);
        }
    }
}
