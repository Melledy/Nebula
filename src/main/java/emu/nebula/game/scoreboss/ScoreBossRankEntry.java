package emu.nebula.game.scoreboss;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;

import emu.nebula.database.GameDatabaseObject;
import emu.nebula.game.player.Player;
import emu.nebula.game.tower.StarTowerBuild;
import emu.nebula.game.character.GameCharacter;
import emu.nebula.proto.Public.HonorInfo;
import emu.nebula.proto.ScoreBossRank.ScoreBossRankChar;
import emu.nebula.proto.ScoreBossRank.ScoreBossRankData;
import emu.nebula.proto.ScoreBossRank.ScoreBossRankTeam;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import lombok.Getter;
import lombok.Setter;

@Getter
@Entity(value = "score_boss_rank", useDiscriminator = false)
public class ScoreBossRankEntry implements GameDatabaseObject {
    @Id
    private int playerUid;
    
    private String name;
    private int level;
    private int headIcon;
    private int titlePrefix;
    private int titleSuffix;
    private int[] honor;
    private int score;
    @SuppressWarnings("unused")
    private int stars;
    private IntSet claimedRewards;
    
    private int controlId;
    private Map<Integer, ScoreBossTeamEntry> teams;
    
    @Setter
    private transient int rank;
    
    @Deprecated // Morphia only
    public ScoreBossRankEntry() {
        this.rank = 999;
    }
    
    public ScoreBossRankEntry(Player player, int controlId) {
        this.playerUid = player.getUid();
        this.controlId = controlId;
        this.teams = new HashMap<>();
    }
    
    // TODO replace later
    public int getStars() {
        int stars = 0;
        
        for (var team : this.getTeams().values()) {
            stars += team.getStars();
        }
        
        return stars;
    }
    
    public IntSet getClaimedRewards() {
        if (this.claimedRewards == null) {
            this.claimedRewards = new IntOpenHashSet();
        }
        
        return this.claimedRewards;
    }
    
    public void update(Player player) {
        this.name = player.getName();
        this.level = player.getLevel();
        this.headIcon = player.getHeadIcon();
        this.titlePrefix = player.getTitlePrefix();
        this.titleSuffix = player.getTitleSuffix();
        this.honor = player.getHonor();
    }
    
    public void settle(Player player, StarTowerBuild build, int controlId, int level, int stars, int score, int skillScore) {
        // Update player data
        this.update(player);
        
        // Reset score entry if control id doesn't match
        if (this.controlId != controlId) {
            this.controlId = controlId;
            this.reset();
        }
        
        // Set team entry
        var team = new ScoreBossTeamEntry(player, build, stars, score, skillScore);
        
        // Put in team map
        this.getTeams().put(level, team);
        
        // Calculate score/stars
        this.score = 0;
        this.stars = 0;
        
        for (var t : this.getTeams().values()) {
            this.score += t.getLevelScore();
            this.stars += t.getStars();
        }
    }
    
    private void reset() {
        this.score = 0;
        this.stars = 0;
        this.getClaimedRewards().clear();
        this.getTeams().clear();
    }
    
    // Proto
    
    public ScoreBossRankData toProto() {
        var proto = ScoreBossRankData.newInstance()
                .setId(this.getPlayerUid())
                .setNickName(this.getName())
                .setWorldClass(this.getLevel())
                .setHeadIcon(this.getHeadIcon())
                .setScore(this.getScore())
                .setTitlePrefix(this.getTitlePrefix())
                .setTitleSuffix(this.getTitleSuffix())
                .setRank(this.getRank());
        
        for (int id : this.getHonor()) {
            proto.addHonors(HonorInfo.newInstance().setId(id));
        }
        
        for (var entry : this.getTeams().entrySet()) {
            // Get team and level id from entry
            int levelId = entry.getKey();
            var team = entry.getValue();
            
            // Get team proto
            var info = team.toProto()
                    .setLevelId(levelId);
            
            proto.addTeams(info);
        }
        
        return proto;
    }
    
    // Extra classes
    
    @Getter
    @Entity(useDiscriminator = false)
    public static class ScoreBossTeamEntry {
        private int buildId;
        private int buildScore;
        private int stars;
        private int levelScore;
        private int skillScore;
        private List<ScoreBossCharEntry> characters;
        
        @Deprecated // Morphia only
        public ScoreBossTeamEntry() {
            
        }
        
        public ScoreBossTeamEntry(Player player, StarTowerBuild build, int stars, int score, int skillScore) {
            this.buildId = build.getUid();
            this.buildScore = build.getScore();
            this.stars = stars;
            this.levelScore = score;
            this.skillScore = skillScore;
            this.characters = new ArrayList<>();
            
            for (var charId : build.getCharIds()) {
                var character = player.getCharacters().getCharacterById(charId);
                if (character != null) {
                    this.getCharacters().add(new ScoreBossCharEntry(character));
                }
            }
        }
        
        public ScoreBossRankTeam toProto() {
            var proto = ScoreBossRankTeam.newInstance()
                    .setBuildScore(this.getBuildScore())
                    .setLevelScore(this.getLevelScore());
            
            for (var c : this.getCharacters()) {
                proto.addChars(c.toProto());
            }
            
            return proto;
        }
    }
    
    @Getter
    @Entity(useDiscriminator = false)
    public static class ScoreBossCharEntry {
        private int id;
        private int level;
        
        @Deprecated // Morphia only
        public ScoreBossCharEntry() {
            
        }
        
        public ScoreBossCharEntry(GameCharacter character) {
            this.id = character.getCharId();
            this.level = character.getLevel();
        }
        
        public ScoreBossRankChar toProto() {
            var proto = ScoreBossRankChar.newInstance()
                    .setId(this.getId())
                    .setLevel(this.getLevel());
            
            return proto;
        }
    }
}
