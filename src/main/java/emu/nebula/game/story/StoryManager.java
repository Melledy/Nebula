package emu.nebula.game.story;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import emu.nebula.Nebula;
import emu.nebula.data.GameData;
import emu.nebula.database.GameDatabaseObject;
import emu.nebula.game.player.Player;
import emu.nebula.game.player.PlayerChangeInfo;
import emu.nebula.game.player.PlayerManager;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

import lombok.Getter;

@Getter
@Entity(value = "story", useDiscriminator = false)
public class StoryManager extends PlayerManager implements GameDatabaseObject {
    @Id
    private int uid;
    
    private IntSet completedStories;
    private Int2IntMap completedSets;
    
    @Deprecated // Morphia only
    public StoryManager() {
        
    }
    
    public StoryManager(Player player) {
        super(player);
        this.uid = player.getUid();
        this.completedStories = new IntOpenHashSet();
        this.completedSets = new Int2IntOpenHashMap();
        
        this.save();
    }
    
    public boolean hasNew() {
        if (this.getCompletedStories().size() < GameData.getStoryDataTable().size()) {
            return true;
        }
        
        if (this.getCompletedSets().size() < GameData.getStorySetSectionDataTable().size()) {
            return true;
        }
        
        return false;
    }

    public PlayerChangeInfo settle(IntList list) {
        // Player change info
        var changes = new PlayerChangeInfo();
        
        for (int id : list) {
            // Get story data
            var data = GameData.getStoryDataTable().get(id);
            if (data == null) continue;
            
            // Check if we already completed the story
            if (this.getCompletedStories().contains(id)) {
                continue;
            }
            
            // Complete story and get rewards
            this.getCompletedStories().add(id);
            
            // Add rewards
            this.getPlayer().getInventory().addItems(data.getRewards(), changes);
            
            // Save to db
            Nebula.getGameDatabase().addToSet(this, this.getPlayerUid(), "completedStories", id);
        }
        
        // Complete
        return changes;
    }

    public PlayerChangeInfo settleSet(int chapterId, int sectionId) {
        // Player change info
        var changes = new PlayerChangeInfo();
        
        // Get story data
        var data = GameData.getStorySetSectionDataTable().get(sectionId);
        if (data == null) return changes;
        
        int sectionIndex = sectionId % 10;
        
        // Check if we already completed the story
        if (this.getCompletedSets().get(chapterId) >= sectionIndex) {
            return changes;
        }
        
        // Complete story and get rewards
        this.getCompletedSets().put(chapterId, sectionIndex);
        
        // Add rewards
        this.getPlayer().getInventory().addItems(data.getRewards(), changes);
        
        // Save to db
        Nebula.getGameDatabase().update(this, this.getPlayerUid(), "completedSets." + chapterId, sectionIndex);
        
        // Complete
        return changes;
    }
}
