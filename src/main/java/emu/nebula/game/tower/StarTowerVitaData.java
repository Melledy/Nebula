package emu.nebula.game.tower;

import java.util.HashMap;
import java.util.Map;

import dev.morphia.annotations.Entity;
import emu.nebula.GameConstants;
import emu.nebula.Nebula;
import emu.nebula.game.player.PlayerChangeInfo;
import emu.nebula.game.player.PlayerProgress;
import lombok.Getter;

@Getter
@Entity(useDiscriminator = false)
public class StarTowerVitaData {
    private int selectedProjectId;
    private Map<Integer, StarTowerVitaProject> projects;

    private transient PlayerProgress progress;

    @Deprecated // Morphia only
    public StarTowerVitaData() {
        
    }
    
    public StarTowerVitaData(PlayerProgress progress) {
        this.setPlayerProgress(progress);
        this.selectedProjectId = 1;
        this.projects = new HashMap<>();
    }

    public void setPlayerProgress(PlayerProgress progress) {
        this.progress = progress;
    }

    public StarTowerVitaProject getProject(int id) {
        return this.getProjects().get(id);
    }

    public void putProject(StarTowerVitaProject project) {
        this.getProjects().put(project.getId(), project);

        // Save projects to the progress document
        this.save("projects." + project.getId(), project);
    }

    public int getSelectedProjectId() {
        return this.selectedProjectId > 0 ? this.selectedProjectId : 1;
    }

    public void setSelectedProjectId(int id) {
        this.selectedProjectId = id;

        // Save selection to the progress document
        this.save("selectedProjectId", id);
    }

    // Vita energy

    public int getEnergy() {
        if (this.progress == null) {
            return 0;
        }

        return this.progress.getPlayer().getInventory().getResourceCount(GameConstants.VITA_ENERGY_ITEM_ID);
    }

    public int addEnergy(int amount, PlayerChangeInfo change) {
        // Sanity check
        if (amount <= 0 || this.progress == null) {
            return 0;
        }

        // Clamp to the energy cap
        int gain = Math.min(amount, GameConstants.VITA_ENERGY_MAX - this.getEnergy());

        if (gain <= 0) {
            return 0;
        }

        // Add energy
        this.progress.getPlayer().getInventory().addItem(GameConstants.VITA_ENERGY_ITEM_ID, gain, change);

        // Return gained amount
        return gain;
    }

    public PlayerChangeInfo buyEnergy(int count) {
        // Sanity check
        if (count <= 0) {
            return null;
        }

        var inventory = this.getProgress().getPlayer().getInventory();

        // Calculate cost
        int cost = GameConstants.VITA_ENERGY_PURCHASE_PRICE * count;

        // Make sure we have enough currency
        if (!inventory.hasItem(GameConstants.VITA_ENERGY_PURCHASE_CURRENCY_ID, cost)) {
            return null;
        }

        // Nothing to gain if we are already at the cap
        if (this.getEnergy() >= GameConstants.VITA_ENERGY_MAX) {
            return null;
        }

        // Buy energy
        var change = inventory.removeItem(GameConstants.VITA_ENERGY_PURCHASE_CURRENCY_ID, cost);
        
        // Add energy that we bought (addEnergy caps the amount at the max)
        int amount = GameConstants.VITA_ENERGY_PURCHASE_VALUE * count;
        inventory.addItem(GameConstants.VITA_ENERGY_ITEM_ID, amount, change);

        // Success
        return change.setSuccess(true);
    }

    // Database

    private void save(String field, Object value) {
        if (this.progress == null) {
            return;
        }

        Nebula.getGameDatabase().update(
            this.progress,
            this.progress.getUid(),
            "towerVitaData." + field,
            value
        );
    }
}
