package emu.nebula.game.story;

import java.util.HashMap;
import java.util.Map;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.PostLoad;
import emu.nebula.Nebula;
import emu.nebula.data.GameData;
import emu.nebula.database.GameDatabaseObject;
import emu.nebula.game.achievement.AchievementCondition;
import emu.nebula.game.player.Player;
import emu.nebula.game.player.PlayerChangeInfo;
import emu.nebula.game.player.PlayerManager;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.PlayerData.PlayerInfo;
import emu.nebula.proto.Public.HandbookInfo;
import emu.nebula.proto.Public.Story;
import emu.nebula.proto.StorySett.StorySettle;
import emu.nebula.util.Bitset;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

import lombok.Getter;
import us.hebi.quickbuf.RepeatedInt;
import us.hebi.quickbuf.RepeatedMessage;

@Getter
@Entity(value = "story", useDiscriminator = false)
public class StoryManager extends PlayerManager implements GameDatabaseObject {
    @Id
    private int uid;
    
    private IntSet completedStories;
    private Int2IntMap completedSets;
    private IntSet evidences;
    
    // Note: Story options are seperate from regular story ids to save database space, since most stories do not have options
    private Map<Integer, StoryOptionLog> options;
    
    // Current story
    private transient int storyId;
    
    @Deprecated // Morphia only
    public StoryManager() {
        
    }
    
    public StoryManager(Player player) {
        super(player);
        this.uid = player.getUid();
        this.completedStories = new IntOpenHashSet();
        this.completedSets = new Int2IntOpenHashMap();
        this.evidences = new IntOpenHashSet();
        this.options = new HashMap<>();
        
        this.save();
    }
    
    public void apply(int idx) {
        this.storyId = idx;
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

    public PlayerChangeInfo settle(RepeatedMessage<StorySettle> list, RepeatedInt evidences) {
        // Player change info
        var change = new PlayerChangeInfo();
        boolean updateHandbook = false;
        
        // Handle regular story
        for (var settle : list) {
            // Get story id
            int id = settle.getIdx();
            
            // Get story data
            var data = GameData.getStoryDataTable().get(id);
            if (data == null) continue;
            
            // Settle options
            // Important: Must be before the completion check as we need to do the same story multiple times to get all the endings
            this.settleOptions(settle);
            
            // Check if we already completed the story
            if (this.getCompletedStories().contains(id)) {
                continue;
            }
            
            // Complete story and get rewards
            this.getCompletedStories().add(id);
            
            // Add rewards
            this.getPlayer().getInventory().addItems(data.getRewards(), change);
            
            // Trigger any achievement/quests
            this.getPlayer().trigger(AchievementCondition.StoryClear, 1, id, 0);
            
            // Save to db
            Nebula.getGameDatabase().addToSet(this, this.getPlayerUid(), "completedStories", id);
        }
        
        // Handle evidences
        for (int id : evidences) {
            // Verify that evidence id exists
            var data = GameData.getStoryEvidenceDataTable().get(id);
            if (data == null) continue;
            
            // Sanity check
            if (this.getEvidences().contains(id)) {
                continue;
            }
            
            // Add evidence
            this.getEvidences().add(id);
            
            // Save to db
            Nebula.getGameDatabase().addToSet(this, this.getPlayerUid(), "evidences", id);
        }
        
        // Update handbook
        if (updateHandbook) {
            this.getPlayer().addNextPackage(
                NetMsgId.handbook_change_notify,
                this.getCgHandbook()
            );
        }
        
        // Clear current story
        this.storyId = 0;
        
        // Complete
        return change;
    }
    
    private void settleOptions(StorySettle settle) {
        // Init variables
        boolean changed = false;
        StoryOptionLog log = null;
        
        // Update
        if (settle.hasMajor()) {
            if (log == null) {
                log = getOptions().computeIfAbsent(settle.getIdx(), idx -> new StoryOptionLog());
            }
            
            if (log.settleMajor(settle.getMajor())) {
                changed = true;
            }
        }
        
        if (settle.hasPersonality()) {
            if (log == null) {
                log = getOptions().computeIfAbsent(settle.getIdx(), idx -> new StoryOptionLog());
            }
            
            if (log.settlePersonality(settle.getPersonality())) {
                changed = true;
            }
        }
        
        // Save to database if we changed anything
        if (changed) {
            Nebula.getGameDatabase().update(this, this.getPlayerUid(), "options." + settle.getIdx(), log);
        }
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
    
    // Handbook
    
    public HandbookInfo getCgHandbook() {
        var bitset = new Bitset();
        
        if (Nebula.getConfig().getServerOptions().unlockAllStoryCGs) {
            for (var data : GameData.getMainScreenCGDataTable()) {
                // Get handbook data
                var handbookData = GameData.getHandbookDataTable().get(data.getId());
                if (handbookData == null || handbookData.getType() != 3) {
                    continue;
                }
                
                // Set flag
                bitset.setBit(handbookData.getIndex());
            }
        }
        
        var handbook = HandbookInfo.newInstance()
                .setType(3)
                .setData(bitset.toByteArray());
        
        return handbook;
    }
    
    // Proto
    
    public void encodePlayerInfo(PlayerInfo proto) {
        var story = proto.getMutableStory();
        
        for (int storyId : this.getCompletedStories()) {
            var storyProto = Story.newInstance()
                    .setIdx(storyId);
            
            var storyOptions = this.getOptions().get(storyId);
            if (storyOptions != null) {
                storyOptions.encodeStoryProto(storyProto);
            }
            
            story.addStories(storyProto);
        }
        
        for (int id : this.getEvidences()) {
            story.addEvidences(id);
        }
    }
    
    // Database fixes
    
    @PostLoad
    public void onLoad() {
        // Simple flag to check if we need to save this manager to the database
        boolean save = false;
        
        // Create evidences/option data if we are missing them
        if (this.evidences == null) {
            this.evidences = new IntOpenHashSet();
            save = true;
        }
        
        if (this.options == null) {
            this.options = new HashMap<>();
            save = true;
        }
        
        // Hardcoded fix if the player is missing the last option in main story chapter 9
        if (this.getCompletedStories().contains(1025)) {
            if (!this.getEvidences().contains(1004)) {
                this.getEvidences().add(1004);
                save = true;
            }
            if (!this.getEvidences().contains(1007)) {
                this.getEvidences().add(1007);
                save = true;
            }
        }
        
        // Save to database
        if (save) {
            this.save();
        }
    }
}
