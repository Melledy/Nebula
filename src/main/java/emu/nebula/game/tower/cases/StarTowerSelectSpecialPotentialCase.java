package emu.nebula.game.tower.cases;

import java.util.List;

import emu.nebula.game.tower.StarTowerGame;
import emu.nebula.game.tower.StarTowerPotentialInfo;
import emu.nebula.proto.PublicStarTower.StarTowerRoomCase;

import lombok.Getter;

@Getter
public class StarTowerSelectSpecialPotentialCase extends StarTowerPotentialCase {
    
    public StarTowerSelectSpecialPotentialCase(StarTowerGame game, int charId, List<StarTowerPotentialInfo> potentials) {
        super(game, charId, potentials);
    }

    @Override
    public CaseType getType() {
        return CaseType.SelectSpecialPotential;
    }
    
    public boolean isRare() {
        return true;
    }
    
    // Proto
    
    @Override
    public void encodeProto(StarTowerRoomCase proto) {
        var select = proto.getMutableSelectSpecialPotentialCase()
            .setTeamLevel(this.getTeamLevel());
        
        for (var potential : this.getPotentials()) {
            select.addIds(potential.getId());
        }
        
        if (this.canReroll()) {
            select.setCanReRoll(true);
            select.setReRollPrice(this.getRerollPrice());
        }
    }
}
