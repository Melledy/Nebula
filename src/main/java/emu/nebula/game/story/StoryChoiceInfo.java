package emu.nebula.game.story;

import dev.morphia.annotations.Entity;
import lombok.Getter;

@Getter
@Entity(useDiscriminator = false)
public class StoryChoiceInfo {
    private int group;
    private int value;
    
    @Deprecated
    public StoryChoiceInfo() {
        // Morphia only
    }
    
    public StoryChoiceInfo(int group, int value) {
        this.group = group;
        this.value = value;
    }

    public StoryChoiceInfo setValue(int value) {
        this.value = value;
        return this;
    }
}
