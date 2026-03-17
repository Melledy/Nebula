package emu.nebula.game.activity.type;

import java.util.HashMap;
import java.util.Map;

import dev.morphia.annotations.Entity;

import emu.nebula.data.GameData;
import emu.nebula.data.resources.ActivityDef;
import emu.nebula.game.activity.ActivityManager;
import emu.nebula.game.activity.GameActivity;
import emu.nebula.proto.ActivityDetail.ActivityMsg;
import emu.nebula.proto.Public.ActivityPenguinCardLevel;
import emu.nebula.proto.Public.ActivityQuest;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.Getter;
import lombok.Setter;

@Getter
@Entity
public class PenguinCardActivity extends GameActivity {
    private Map<Integer, LevelStats> completedLevels = new Int2ObjectOpenHashMap<>();
    private Int2IntMap completedQuests = new Int2IntOpenHashMap();
    
    @Deprecated // Morphia only
    public PenguinCardActivity() {
        
    }
    
    public PenguinCardActivity(ActivityManager manager, ActivityDef data) {
        super(manager, data);
    }
    
    // Proto

    @Override
    public void encodeActivityMsg(ActivityMsg msg) {
        var proto = msg.getMutablePenguinCard();

        // Add completed levels
        for (int id : this.completedLevels.keySet()) {
            // Get level completion data
            var data = this.completedLevels.get(id);

            // Create proto
            var level = ActivityPenguinCardLevel.newInstance();

            // Set proto params
            level.setId(id);
            level.setScore(data.getScore());
            level.setStar(data.getStars());

            // Add to final msg proto
            proto.addLevels(level);
        }

        // Add completed quests
        for (int id : this.completedQuests.keySet()) {
            // Create proto
            var quest = ActivityQuest.newInstance();

            // Set proto params
            quest.setActivityId(this.getId());
            quest.setId(id);
            quest.setStatus(2);     // TODO: properly handle event quests

            // Add to final msg proto
            proto.addQuests(quest);
        }
    }

    @Getter
    @Setter
    @Entity(useDiscriminator = false)
    public static class LevelStats {
        private int stars;
        private int score;

        @Deprecated
        public LevelStats() {

        }
    }

}
