package emu.nebula.command;

import java.util.List;

import emu.nebula.Nebula;
import emu.nebula.game.character.GameCharacter;
import emu.nebula.game.character.GameDisc;
import emu.nebula.game.player.Player;
import emu.nebula.util.Utils;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import lombok.Getter;

@Getter
public class CommandArgs {
    private String raw;
    private List<String> list;
    private Player sender;
    private Player target;
    
    private int targetUid;
    private int amount;
    private int level = -1;
    private int advance = -1;
    private int talent = -1;
    private int skill = -1;
    private int affinity = -1;
    
    private Int2IntMap map;
    private ObjectSet<String> flags;

    public CommandArgs(Player sender, List<String> args) {
        this.sender = sender;
        this.raw = String.join(" ", args);
        this.list = args;
        
        // Parse args. Maybe regex is better.
        var it = this.list.iterator();
        while (it.hasNext()) {
            // Lower case first
            String arg = it.next().toLowerCase();
            
            try {
                if (arg.length() >= 2 && !Character.isDigit(arg.charAt(0)) && Character.isDigit(arg.charAt(arg.length() - 1))) {
                    if (arg.startsWith("@")) { // Target UID
                        this.targetUid = Utils.parseSafeInt(arg.substring(1));
                        it.remove();
                    } else if (arg.startsWith("x")) { // Amount
                        this.amount = Utils.parseSafeInt(arg.substring(1));
                        it.remove();
                    } else if (arg.startsWith("lv")) { // Level
                        this.level = Utils.parseSafeInt(arg.substring(2));
                        it.remove();
                    } else if (arg.startsWith("a")) { // Advance
                        this.advance = Utils.parseSafeInt(arg.substring(1));
                        it.remove();
                    } else if (arg.startsWith("t") || arg.startsWith("c")) { // Talents/Crescendo
                        this.talent = Utils.parseSafeInt(arg.substring(1));
                        it.remove();
                    } else if (arg.startsWith("s")) { // Skill
                        this.skill = Utils.parseSafeInt(arg.substring(1));
                        it.remove();
                    } else if (arg.startsWith("f")) { // Affinity level
                        this.affinity = Utils.parseSafeInt(arg.substring(1));
                        it.remove();
                    }
                } else if (arg.startsWith("-")) { // Flag
                    if (this.flags == null) this.flags = new ObjectOpenHashSet<>();
                    this.flags.add(arg);
                    it.remove();
                } else if (arg.contains(":") || arg.contains(",")) {
                    String[] split = arg.split("[:,]");
                    if (split.length >= 2) {
                        int key = Integer.parseInt(split[0]);
                        int value = Integer.parseInt(split[1]);
                        
                        if (this.map == null) this.map = new Int2IntOpenHashMap();
                        this.map.put(key, value);
                        
                        it.remove();
                    }
                }
            } catch (Exception e) {
                
            }
        }
        
        // Get target player
        if (targetUid != 0) {
            if (Nebula.getGameContext() != null) {
                target = Nebula.getGameContext().getPlayerModule().getCachedPlayerByUid(targetUid);
            }
        } else {
            target = sender;
        }
        
        if (target != null) {
            this.targetUid = target.getUid();
        }
    }
    
    public int size() {
        return this.list.size();
    }
    
    public String get(int index) {
        if (index < 0 || index >= list.size()) {
            return "";
        }
        
        return this.list.get(index);
    }
    
    /**
     * Sends a message to the command sender
     * @param message
     */
    public void sendMessage(String message) {
        if (sender != null) {
            sender.sendMessage(message);
        } else {
            Nebula.getLogger().info(message);
        }
    }
    
    public boolean hasFlag(String flag) {
        if (this.flags == null) return false;
        return this.flags.contains(flag);
    }
    
    // Utility commands
    
    /**
     * Changes the properties of an character based on the arguments provided
     * @param character The targeted character to change
     * @return A boolean of whether or not any changes were made to the character
     */
    public boolean setProperties(GameCharacter character) {
        boolean hasChanged = false;

        // Try to set level
        int level = Math.min(this.getLevel(), 90);
        
        if (level > 0 && character.getLevel() != level) {
            character.setLevel(level);
            character.setAdvance(Utils.getMinAdvanceForLevel(character.getLevel()));
            hasChanged = true;
        }
        
        // Try to set advance (ascension level)
        int advance = Math.min(this.getAdvance(), 8);
        
        if (advance >= 0 && character.getAdvance() != advance) {
            character.setAdvance(advance);
            hasChanged = true;
        }
        
        // Try to set skill trees
        if (this.getSkill() > 0) {
            int skill = Math.min(this.getSkill(), 10);
            
            for (int i = 0; i < 4; i++) {
                int s = character.getSkills()[i];
                
                if (s != skill) {
                    character.getSkills()[i] = skill;
                    hasChanged = true;
                }
            }
        }
        
        // Try to set talents
        if (this.getTalent() >= 0) {
            // Clear talents first
            character.getTalents().clear();
            
            // Calculate how many talent stars we want to set
            int talent = Math.min(this.getTalent(), 5);
            
            for (int i = 0; i < talent; i++) {
                // Get bitset offset
                int offset = i * 16;
                
                // First 10 sub nodes of a talent star
                for (int x = 1; x <= 10; x++) {
                    character.getTalents().setBit(offset + x);
                }
                
                // Final sub node of a talent star
                character.getTalents().setBit(offset + 16);
            }
            
            hasChanged = true;
        }

        if (this.getAffinity() >= 0) {
            int target = this.getAffinity();
            if (target > 50) target = 50;
            if (target < 0) target = 0;
            if (character.getAffinityLevel() != target) {
                character.setAffinityLevel(target);
                hasChanged = true;
            }
        }
        
        return hasChanged;
    }
    
    /**
     * Changes the properties of an disc based on the arguments provided
     * @param disc The targeted disc to change
     * @return A boolean of whether or not any changes were made to the disc
     */
    public boolean setProperties(GameDisc disc) {
        boolean hasChanged = false;

        // Try to set level
        int level = Math.min(this.getLevel(), 90);
        
        if (level > 0 && disc.getLevel() != level) {
            disc.setLevel(level);
            disc.setPhase(Utils.getMinAdvanceForLevel(disc.getLevel()));
            hasChanged = true;
        }
        
        // Try to set advance (ascension level)
        int promotion = Math.min(this.getAdvance(), 8);
        
        if (promotion >= 0 && disc.getPhase() != promotion) {
            disc.setPhase(promotion);
            hasChanged = true;
        }
        
        // Calculate how many talent stars we want to set
        int star = Math.min(this.getTalent(), 5);
        
        if (star >= 0 && disc.getStar() != star) {
            disc.setStar(star);
            hasChanged = true;
        }
        
        return hasChanged;
    }
}
