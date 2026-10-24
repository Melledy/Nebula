package emu.nebula.game.tower;

import dev.morphia.annotations.Entity;
import emu.nebula.Nebula;
import emu.nebula.data.GameData;
import emu.nebula.data.resources.NpcAffinityGroupDef;
import emu.nebula.game.player.Player;
import emu.nebula.game.quest.QuestCondition;
import emu.nebula.proto.Public.NPCAffinity;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import lombok.Getter;

@Getter
@Entity(useDiscriminator = false)
public class StarTowerNpcData {
    private int id;
    private int level;
    private int affinity;
    private IntSet plotIds;
    
    // Cached reference to the player
    private transient Player player;
    
    @Deprecated // Morphia only
    public StarTowerNpcData() {
        
    }
    
    public StarTowerNpcData(Player player, int id) {
        this.id = id;
        this.plotIds = new IntOpenHashSet();
        this.setPlayer(player);
        this.save();
    }
    
    public void setPlayer(Player player) {
        this.player = player;
        
        // Recalculate the level in case the npc was created before levels were stored
        this.refreshLevel();
    }
    
    public IntSet getPlotIds() {
        return this.plotIds;
    }
    
    /**
     * Gets the affinity data for this NPC at the specified level.
     */
    private NpcAffinityGroupDef getAffinityGroupData(int level) {
        var data = GameData.getStarTowerNpcDataTable().get(this.getId());
        if (data == null) {
            return null;
        }
        
        return GameData.getNpcAffinityGroupDataTable().get((data.getAffinityGroupId() * 100) + level);
    }
    
    /**
     * Calculates the NPC's affinity level from its current affinity.
     */
    private int calculateLevel() {
        int level = this.level;
        var nextLevel = this.getAffinityGroupData(level + 1);
        
        // Level up while the player has enough affinity for the next level
        while (nextLevel != null && nextLevel.getAffinityValue() > 0 && this.affinity >= nextLevel.getAffinityValue()) {
            level++;
            nextLevel = this.getAffinityGroupData(level + 1);
        }
        
        return level;
    }
    
    /**
     * Updates the cached level from the NPC's current affinity.
     * @return True if the level changed
     */
    public boolean refreshLevel() {
        int level = this.calculateLevel();
        
        if (this.level == level) {
            return false;
        }
        
        this.level = level;
        
        return true;
    }
    
    /**
     * Returns the maximum amount of affinity this NPC can have.
     */
    public int getMaxAffinity() {
        int maxAffinity = 0;
        int level = 0;
        var data = this.getAffinityGroupData(level);
        
        // Only iterate the affinity levels for this NPC's group
        while (data != null) {
            maxAffinity = Math.max(maxAffinity, data.getAffinityValue());
            
            level++;
            data = this.getAffinityGroupData(level);
        }
        
        return maxAffinity;
    }
    
    public int addAffinity(int amount) {
        if (amount <= 0) {
            return 0;
        }
        
        // Save old affinity
        int oldAffinity = this.affinity;
        int oldLevel = this.level;
        
        // Cap affinity at the maximum level for this npc
        int newAffinity = this.affinity + amount;
        int maxAffinity = this.getMaxAffinity();
        
        if (maxAffinity > 0) {
            newAffinity = Math.min(newAffinity, maxAffinity);
        }
        
        this.affinity = newAffinity;
        this.refreshLevel();
        
        // Return changed affinity amount
        int change = this.affinity - oldAffinity;
        
        if (change > 0 || this.level != oldLevel) {
            this.save();
        }
        
        // Trigger affinity level quests for each level gained
        if (this.level > oldLevel && this.player != null) {
            this.player.trigger(QuestCondition.NpcAffinityWithSpecificLevel, this.level - oldLevel, this.getId());
        }
        
        return change;
    }
    
    public boolean hasPlot(int plotId) {
        return this.getPlotIds().contains(plotId);
    }
    
    public boolean addPlot(int plotId) {
        if (!this.getPlotIds().add(plotId)) {
            return false;
        }
        
        this.save();
        
        return true;
    }
    
    // Proto
    
    public NPCAffinity toProto() {
        var proto = NPCAffinity.newInstance()
                .setNPCId(this.getId())
                .setAffinity(this.getAffinity());
        
        for (int plotId : this.getPlotIds()) {
            proto.addPlotIds(plotId);
        }
        
        return proto;
    }
    
    // Database
    
    /**
     * Saves this npc data to the database
     */
    public void save() {
        Nebula.getGameDatabase().update(player.getProgress(), player.getUid(), "towerNpcs." + this.getId(), this, true);
    }
    
}
