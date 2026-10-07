package emu.nebula.game.story;

import java.util.ArrayList;
import java.util.List;

import dev.morphia.annotations.Entity;

import emu.nebula.proto.Public.Story;
import emu.nebula.proto.Public.StoryChoice;
import emu.nebula.proto.StorySett.StoryOptions;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import lombok.Getter;

import us.hebi.quickbuf.RepeatedMessage;

@Getter
@Entity(useDiscriminator = false)
public class StoryOptionLog {
    private List<StoryChoiceInfo> major;
    private List<StoryChoiceInfo> personality;
    
    public StoryOptionLog() {
        
    }
    
    public int getMajorOptionSize() {
        if (this.major == null) {
            return 0;
        }
        
        return this.major.size();
    }
    
    public boolean hasMajorOption(int group, int choice) {
        if (this.major == null) {
            return false;
        }
        
        return this.major.stream()
                .filter(c -> c.getGroup() == group && c.getValue() == choice)
                .findFirst()
                .isPresent();
    }
    
    public boolean addMajorOption(int group, int choice) {
        if (this.major == null) {
            this.major = new ArrayList<>();
        }
        
        return this.major.add(new StoryChoiceInfo(group, choice));
    }
    
    public boolean settleMajor(RepeatedMessage<StoryOptions> options) {
        boolean success = false;
        
        for (var option : options) {
            // Sanity check
            if (this.getMajorOptionSize() >= 5) {
                break;
            }
            
            // Skip if we already have this choice
            if (this.hasMajorOption(option.getGroup(), option.getChoice())) {
                continue;
            }
            
            // Add
            this.addMajorOption(option.getGroup(), option.getChoice());
            
            // Set success flag
            success = true;
        }
        
        return success;
    }
    
    public int getPersonalityOptionSize() {
        if (this.personality == null) {
            return 0;
        }
        
        return this.personality.size();
    }
    
    public void addPersonalityOption(int group, int choice) {
        if (this.personality == null) {
            this.personality = new ArrayList<>();
        }
        
        // Try and get existing group option
        var option = this.personality.stream()
            .filter(c -> c.getGroup() == group)
            .findFirst()
            .orElseGet(null);
        
        // Set value for choice option
        if (option == null) {
            this.personality.add(new StoryChoiceInfo(group, choice));
        } else {
            option.setValue(choice);
        }
    }

    public boolean settlePersonality(RepeatedMessage<StoryOptions> options) {
        boolean success = false;
        
        for (var option : options) {
            // Sanity check
            if (this.getPersonalityOptionSize() >= 5) {
                break;
            }
            
            // TODO fix old accounts with multiple of the same personality groups
            
            // Add
            this.addPersonalityOption(option.getGroup(), option.getChoice());
            
            // Set success flag
            success = true;
        }
        
        return success;
    }
    
    // Proto

    public void encodeStoryProto(Story proto) {
        if (this.major != null) {
            var chosen = new Int2IntOpenHashMap();
            
            for (var choice : this.major) {
                var value = chosen.get(choice.getGroup()) | choice.getValue();
                chosen.put(choice.getGroup(), value);
            }
            
            for (var entry : chosen.int2IntEntrySet()) {
                var choice = StoryChoice.newInstance()
                    .setGroup(entry.getIntKey())
                    .setValue(entry.getIntValue());
                
                proto.addMajor(choice);
            }
        }
        
        if (this.personality != null) {
            var chosen = new Int2IntOpenHashMap();
            
            for (var choice : this.personality) {
                chosen.put(choice.getGroup(), choice.getValue());
            }
            
            for (var entry : chosen.int2IntEntrySet()) {
                int factor = 5; // Guessed value
                int value = (entry.getIntValue() << 4) + (factor << 8);
                
                var choice = StoryChoice.newInstance()
                    .setGroup(entry.getIntKey())
                    .setValue(value);
                
                proto.addPersonality(choice);
            }
        }
    }
    
}
