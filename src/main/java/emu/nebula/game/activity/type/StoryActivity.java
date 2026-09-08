package emu.nebula.game.activity.type;

import java.util.HashMap;
import java.util.Map;

import dev.morphia.annotations.Entity;

import emu.nebula.data.GameData;
import emu.nebula.data.resources.ActivityDef;
import emu.nebula.game.activity.ActivityManager;
import emu.nebula.game.activity.GameActivity;
import emu.nebula.game.player.PlayerChangeInfo;
import emu.nebula.proto.ActivityDetail.ActivityMsg;
import emu.nebula.proto.ActivityStorySettleOuterClass.ActivityStorySettle;
import emu.nebula.proto.Public.ActivityStory;
import emu.nebula.proto.Public.ActivityStoryChoice;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

import lombok.Getter;
import lombok.Setter;

import us.hebi.quickbuf.RepeatedInt;

@Getter
@Entity
public class StoryActivity extends GameActivity {
    private Map<Integer, ActivityStoryInfo> stories;
    private IntSet evidences;
    private long buildId;
    
    @Deprecated // Morphia only
    public StoryActivity() {
        
    }
    
    public StoryActivity(ActivityManager manager, ActivityDef data) {
        super(manager, data);
        this.stories = new HashMap<>();
        this.evidences = new IntOpenHashSet();
    }
    
    private int getChapterId() {
        return (int) Math.floor(this.getId() / 100);
    }
    
    public boolean apply(int storyId, long buildId) {
        // Check story id
        var story = GameData.getActivityStoryDataTable().get(storyId);
        
        if (story == null || story.getChapterId() != this.getChapterId()) {
            return false;
        }
        
        // Set build id
        if (buildId != 0) {
            this.buildId = buildId;
        }
        
        return true;
    }
    
    public PlayerChangeInfo settle(Iterable<ActivityStorySettle> list, RepeatedInt evidences) {
        // Create change info
        var change = new PlayerChangeInfo();
        
        // Boolean flag to check if any data was changed/added
        boolean hasChanged = false;
        
        // Handle settle info
        for (var settle : list) {
            // Get story data
            var story = GameData.getActivityStoryDataTable().get(settle.getStoryId());
            
            // Validate story
            if (story == null || story.getChapterId() != this.getChapterId()) {
                continue;
            }
                    
            // Check if we have already completed this story
            if (this.getStories().containsKey(story.getId())) {
                continue;
            }
            
            // Create story info
            var info = new ActivityStoryInfo(story.getId());
            
            for (var option : settle.getMajor()) {
                info.addMajor(option.getGroup(), option.getChoice());
            }
            
            for (var option : settle.getPersonality()) {
                info.addPersonality(option.getGroup(), option.getChoice());
            }

            // Add to story log
            this.stories.put(story.getId(), info);
            
            // Set changed flag
            hasChanged = true;
            
            // Add rewards
            this.getPlayer().getInventory().addItems(story.getRewards(), change);
        }
        
        // Add evidences
        for (int i : evidences) {
            this.getEvidences().add(i);
            
            // Set changed flag
            hasChanged = true;
        }
        
        // Save to database if anything has changed
        if (hasChanged) {
            this.save();
        }
        
        // Success
        return change;
    }
    
    // Proto

    @Override
    public void encodeActivityMsg(ActivityMsg msg) {
        var proto = msg.getMutableStoryChapter()
                .setBuildId(this.getBuildId());

        for (int i : this.getEvidences()) {
            proto.addEvidences(i);
        }
        
        for (var info : this.getStories().values()) {
            proto.addStories(info.toProto());
        }
    }

    @Setter
    @Getter
    @Entity(useDiscriminator = false)
    public static class ActivityStoryInfo {
        private int id;
        private Int2IntMap major;
        private Int2IntMap personality;
        
        @Deprecated // Morphia only
        public ActivityStoryInfo() {
            
        }
        
        public ActivityStoryInfo(int id) {
            this.id = id;
        }
        
        public void addMajor(int group, int choice) {
            if (this.major == null) {
                this.major = new Int2IntOpenHashMap();
            }
            
            this.major.put(group, choice);
        }
        
        public void addPersonality(int group, int choice) {
            if (this.personality == null) {
                this.personality = new Int2IntOpenHashMap();
            }
            
            this.personality.put(group, choice);
        }
        
        // Proto
        
        public ActivityStory toProto() {
            var proto = ActivityStory.newInstance()
                    .setId(this.id);
            
            if (this.major != null) {
                for (var entry : this.major.int2IntEntrySet()) {
                    var choice = ActivityStoryChoice.newInstance()
                            .setGroup(entry.getIntKey())
                            .setValue(entry.getIntValue());
                    
                    proto.addMajor(choice);
                }
            }
            
            if (this.personality != null) {
                for (var entry : this.personality.int2IntEntrySet()) {
                    var choice = ActivityStoryChoice.newInstance()
                            .setGroup(entry.getIntKey())
                            .setValue(entry.getIntValue());
                    
                    proto.addPersonality(choice);
                }
            }
            
            return proto;
        }
    }
}
