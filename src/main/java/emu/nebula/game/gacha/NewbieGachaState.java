package emu.nebula.game.gacha;

import java.util.*;
import dev.morphia.annotations.Entity;
import lombok.Getter;

@Getter
@Entity(useDiscriminator = false)
public class NewbieGachaState {
    private int id;
    private int remainingSpinCount;
    private int saveCount = 1;
    private int selectedResult = -1;
    private boolean received;
    private int[] pendingResult;
    private final List<int[]> savedResults = new ArrayList<>();

    @Deprecated
    public NewbieGachaState() {
    }

    public NewbieGachaState(int id, int spinCount, int saveCount) {
        this.id = id;
        this.remainingSpinCount = Math.max(0, spinCount);
        this.saveCount = Math.max(1, saveCount);
    }

    public boolean hasPendingResult() {
        return pendingResult != null && pendingResult.length > 0;
    }

    // Checks if the player can perform a spin.
    public boolean canSpin(boolean obtainLocked) {
        return !received && !obtainLocked && remainingSpinCount > 0;
    }

    // Checks if the current pending result can be moved to saved results
    public boolean canSavePendingResult(boolean obtainLocked) {
        return !received && !obtainLocked && hasPendingResult();
    }

    // Updates the maximum allowed saved results.
    public boolean applyConfig(int saveCount) {
        int oldSaveCount = this.saveCount;
        this.saveCount = Math.max(1, saveCount);
        return this.saveCount != oldSaveCount;
    }

    // Applies a new spin result to the pending slot and consumes a spin attempt
    public boolean applySpinResult(int[] cards) {
        if (!canSpin(false) || cards == null || cards.length == 0) {
            return false;
        }

        this.pendingResult = cards;
        this.remainingSpinCount--;
        return true;
    }

    // Saves the pending result into the saved results list at the specified index or adds it
    public boolean savePendingResult(int index) {
        if (!hasPendingResult() || received) return false;

        if (index >= 0 && index < savedResults.size()) {
            // Replace existing slot
            savedResults.set(index, pendingResult);
        } else if (savedResults.size() < saveCount) {
            // Add new slot if capacity allows
            savedResults.add(pendingResult);
        } else {
            return false;
        }

        this.pendingResult = null;
        return true;
    }

    // Returns a clone of the saved result at the given index
    public int[] copySavedResult(int index) {
        return (index >= 0 && index < savedResults.size()) ? savedResults.get(index).clone() : null;
    }

    // Check for claiming a specific result
    public boolean canObtain(int index) {
        return !received && index >= 0 && index < savedResults.size();
    }

    // Marks a specific saved result as claimed and closes the gacha session
    public boolean markReceived(int index) {
        if (received || index < 0 || index >= savedResults.size()) {
            return false;
        }

        this.selectedResult = index;
        this.received = true;
        this.remainingSpinCount = 0;
        this.pendingResult = null;
        return true;
    }

}
