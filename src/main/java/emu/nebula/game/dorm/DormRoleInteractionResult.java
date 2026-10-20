package emu.nebula.game.dorm;

import java.util.List;

import emu.nebula.game.character.GameCharacter;
import emu.nebula.game.player.PlayerChangeInfo;

import lombok.Getter;

/**
 * The result of interacting with roles in a dorm room.
 */
@Getter
public class DormRoleInteractionResult {
    // The changes of the interactions
    private final PlayerChangeInfo change;
    
    // The interactions that were performed
    private final List<Interaction> interactions;
    
    public DormRoleInteractionResult(PlayerChangeInfo change, List<Interaction> interactions) {
        this.change = change;
        this.interactions = interactions;
    }
    
    /**
     * A single role interaction.
     */
    public record Interaction(GameCharacter character, int itemId) {
        
    }
}
