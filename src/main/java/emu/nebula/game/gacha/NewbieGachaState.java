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
    public boolean canSpin() {
        return !received && remainingSpinCount > 0;
    }

    // Checks if the current pending result can be moved to saved results
    public boolean canSavePendingResult() {
        return !received && hasPendingResult();
    }

    // Updates the maximum allowed saved results.
    public void applyConfig(int saveCount) {
        this.saveCount = Math.max(1, saveCount);
    }

    // Applies a new spin result to the pending slot and consumes a spin attempt
    public boolean applySpinResult(int[] cards) {
        if (!canSpin() || cards == null || cards.length == 0) {
            return false;
        }

        this.pendingResult = cards;
        this.remainingSpinCount--;
        return true;
    }

    // Saves the pending result into the saved results list at the specified index or adds it
    public boolean savePendingResult(Integer index) {
        if (!hasPendingResult() || this.received) {
            return false;
        }

        if (index != null && index < savedResults.size()) {
            // Replace existing slot
            savedResults.set(index, pendingResult);
        } else if (index == null && savedResults.size() < saveCount) {
            // Add new slot if capacity allows
            savedResults.add(pendingResult);
        } else {
            return false;
        }

        this.pendingResult = null;
        return true;
    }

    // Returns a clone of the result referenced by obtain request index.
    // Idx=0 -> pendingResult, Idx=1-saveCount -> savedResults[idx-1]
    public int[] copySavedResult(int index) {
        if (index == 0) {
            return hasPendingResult() ? pendingResult.clone() : null;
        }

        int savedResultIndex = index - 1;
        return (savedResultIndex >= 0 && savedResultIndex < savedResults.size()) ? savedResults.get(savedResultIndex).clone() : null;
    }

    // Checks whether the obtain request index points to a valid result.
    // Idx=0 -> pendingResult, Idx=1-saveCount -> savedResults[idx-1]
    public boolean canObtain(int index) {
        return !received && copySavedResult(index) != null;
    }

    // Marks the selected obtain request index as claimed and closes the gacha session.
    public boolean markReceived(int index) {
        if (received || copySavedResult(index) == null) {
            return false;
        }

        this.selectedResult = index;
        this.received = true;
        this.remainingSpinCount = 0;
        this.pendingResult = null;
        return true;
    }

}
