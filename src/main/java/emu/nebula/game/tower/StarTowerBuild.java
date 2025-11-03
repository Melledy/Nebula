package emu.nebula.game.tower;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Indexed;
import emu.nebula.Nebula;
import emu.nebula.data.GameData;
import emu.nebula.database.GameDatabaseObject;
import emu.nebula.proto.Public.ItemTpl;
import emu.nebula.proto.PublicStarTower.BuildPotential;
import emu.nebula.proto.PublicStarTower.StarTowerBuildBrief;
import emu.nebula.proto.PublicStarTower.StarTowerBuildDetail;
import emu.nebula.proto.PublicStarTower.StarTowerBuildInfo;
import emu.nebula.proto.PublicStarTower.TowerBuildChar;
import emu.nebula.util.Snowflake;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import lombok.Getter;

@Getter
@Entity(value = "star_tower_builds", useDiscriminator = false)
public class StarTowerBuild implements GameDatabaseObject {
    @Id
    private int uid;
    @Indexed
    private int playerUid;
    
    private String name;
    private boolean lock;
    private boolean preference;
    private int score;
    
    private int[] charIds;
    private int[] discIds;
    
    private Int2IntMap charPots;
    private Int2IntMap potentials;
    private Int2IntMap subNoteSkills;
    
    @Deprecated
    public StarTowerBuild() {
        
    }
    
    public StarTowerBuild(StarTowerGame game) {
        this.uid = Snowflake.newUid();
        this.playerUid = game.getPlayer().getUid();
        this.name = "";
        this.charPots = new Int2IntOpenHashMap();
        this.potentials = new Int2IntOpenHashMap();
        this.subNoteSkills = new Int2IntOpenHashMap();
        
        // Characters
        this.charIds = game.getChars().stream()
                .filter(d -> d.getId() > 0)
                .mapToInt(d -> d.getId())
                .toArray();
        
        // Discs
        this.discIds = game.getDiscs().stream()
                .filter(d -> d.getId() > 0)
                .mapToInt(d -> d.getId())
                .toArray();
        
        // Add potentials
        for (var entry : game.getPotentials()) {
            //
            int id = entry.getIntKey();
            int level = entry.getIntValue();
            
            // Add to potential map
            this.getPotentials().put(id, level);
            
            // Add to character
            var potentialData = GameData.getPotentialDataTable().get(id);
            if (potentialData != null) {
                int charId = potentialData.getCharId();
                this.getCharPots().put(charId, this.getCharPots().get(charId) + 1);
            }
        }
        
        // Add sub note skills
        for (var entry : game.getItems()) {
            this.getSubNoteSkills().put(entry.getIntKey(), entry.getIntValue());
        }
    }

    public void setName(String newName) {
        if (newName.length() > 32) {
            newName = newName.substring(0, 31);
        }
        
        this.name = newName;
        Nebula.getGameDatabase().update(this, this.getUid(), "name", this.getName());
    }
    
    public void setLock(boolean state) {
        this.lock = state;
        Nebula.getGameDatabase().update(this, this.getUid(), "lock", this.isLock());
    }
    
    public void setPreference(boolean state) {
        this.preference = state;
        Nebula.getGameDatabase().update(this, this.getUid(), "preference", this.isPreference());
    }
    
    // Proto
    
    public StarTowerBuildInfo toProto() {
        var proto = StarTowerBuildInfo.newInstance()
                .setBrief(this.toBriefProto())
                .setDetail(this.toDetailProto());

        return proto;
    }

    public StarTowerBuildBrief toBriefProto() {
        var proto = StarTowerBuildBrief.newInstance()
                .setId(this.getUid())
                .setName(this.getName())
                .setLock(this.isLock())
                .setPreference(this.isPreference())
                .setScore(this.getScore())
                .addAllDiscIds(this.getDiscIds());
        
        // Add characters
        for (int charId : this.getCharIds()) {
            var charProto = TowerBuildChar.newInstance()
                    .setCharId(charId)
                    .setPotentialCnt(this.getCharPots().get(charId));
            
            proto.addChars(charProto);
        }
        
        return proto;
    }
    
    public StarTowerBuildDetail toDetailProto() {
        var proto = StarTowerBuildDetail.newInstance();
        
        // Potentials
        for (var entry : this.getPotentials().int2IntEntrySet()) {
            var potential = BuildPotential.newInstance()
                    .setPotentialId(entry.getIntKey())
                    .setLevel(entry.getIntValue());
            
            proto.getMutablePotentials().add(potential);
        }
        
        // Sub note skills
        for (var entry : this.getSubNoteSkills().int2IntEntrySet()) {
            var skill = ItemTpl.newInstance()
                    .setTid(entry.getIntKey())
                    .setQty(entry.getIntValue());
            
            proto.addSubNoteSkills(skill);
        }
        
        return proto;
    }
    
    // Database

    public void delete() {
        Nebula.getGameDatabase().delete(this);
    }
}
