package emu.nebula.command;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

import org.bouncycastle.util.encoders.Base64;

import emu.nebula.Nebula;
import emu.nebula.data.GameData;
import emu.nebula.data.resources.AffinityLevelDef;
import emu.nebula.game.character.GameCharacter;
import emu.nebula.game.character.GameDisc;
import emu.nebula.game.player.Player;
import emu.nebula.util.Utils;

import it.unimi.dsi.fastutil.ints.Int2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
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
                    } else if (arg.startsWith("lvl")) { // Level
                        this.level = Utils.parseSafeInt(arg.substring(3));
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
                        
                        if (this.map == null) this.map = new Int2IntLinkedOpenHashMap();
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
            if (target > AffinityLevelDef.getMaxLevel()) target = AffinityLevelDef.getMaxLevel();
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
    
    /**
     * Converts a preset code back into a map of ids
     * @param code
     * @return
     */
    public Int2IntMap parsePresetCode(String code) {
        // Init map
        var items = new Int2IntLinkedOpenHashMap();
        
        // Decode preset data from base64
        byte[] data = Base64.decode(code);
        
        var preset = ByteBuffer.wrap(data);
        preset.order(ByteOrder.BIG_ENDIAN);
        
        // Parse character ids
        int[] charIds = new int[3];
        try {
            for (int i = 0; i < 3; i++) {
                int id = preset.getInt();
                
                charIds[i] = id;
                items.put(id, 1);
            }
        } catch (Exception e) {
            // Ignored
            return null;
        }
        
        // Parse potentials
        try {
            for (int i = 0; i < 3; i++) {
                int charId = charIds[i];
                
                // Get potential data
                var potentials = GameData.getCharPotentialDataTable().get(charId);
                if (potentials == null) {
                    return null;
                }
                
                // Setup potentials
                int[] specificPotentialIds;
                int[] normalPotentialIds;
                int[] commonPotentialIds = potentials.getCommonPotentialIds();
                
                // Check if main trekker or not
                if (i == 0) {
                    specificPotentialIds = potentials.getMasterSpecificPotentialIds();
                    normalPotentialIds = potentials.getMasterNormalPotentialIds();
                } else {
                    specificPotentialIds = potentials.getAssistSpecificPotentialIds();
                    normalPotentialIds = potentials.getAssistNormalPotentialIds();
                }
                
                // Parser
                int size = (int) Math.ceil((specificPotentialIds.length + (normalPotentialIds.length * 3) + (commonPotentialIds.length * 3)) / 8D);
                byte[] potentialBytes = new byte[size];
                preset.get(potentialBytes);
                var potentialData = new BitReader(potentialBytes);
                
                // Calculate levels of potentials
                for (int id : specificPotentialIds) {
                    int level = potentialData.readBits(1);
                    if (level > 0) {
                        items.put(id, level);
                    }
                }
                
                for (int id : normalPotentialIds) {
                    int level = potentialData.readBits(3);
                    if (level > 0) {
                        items.put(id, level);
                    }
                }
                
                for (int id : commonPotentialIds) {
                    int level = potentialData.readBits(3);
                    if (level > 0) {
                        items.put(id, level);
                    }
                }
            }
        } catch (Exception e) {
            // Ignored
            return null;
        }
        
        // Finished
        return items;
    }
    
    @Getter
    private static class BitReader {
        private byte[] bits;
        private int arrayIndex;
        private int bitIndex;
        
        public BitReader(byte[] bits) {
            this.bits = bits;
        }
        
        public int readBits(int count) {
            int result = 0;
            for (int i = 0; i < count; i++) {
                result <<= 1;
                result |= (this.bits[arrayIndex] >> (7 - bitIndex)) & 1;
                bitIndex++;
                if (bitIndex >= 8) {
                    bitIndex = 0;
                    arrayIndex++;
                }
            }
            return result;
        }
    }
}
