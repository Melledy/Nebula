package emu.nebula.game.tower.cases;

import emu.nebula.proto.PublicStarTower.StarTowerRoomCase;
import emu.nebula.proto.StarTowerInteract.StarTowerInteractReq;
import emu.nebula.proto.StarTowerInteract.StarTowerInteractResp;
import lombok.Getter;

@Getter
public class StarTowerNpcRecoveryHPCase extends StarTowerBaseCase {
    private int effectId;
    
    public StarTowerNpcRecoveryHPCase() {
        this(989970); // Restore Hp/Energy by 50%
    }
    
    public StarTowerNpcRecoveryHPCase(int effectId) {
        this.effectId = effectId;
    }
    
    @Override
    public CaseType getType() {
        return CaseType.NpcRecoveryHP;
    }
    
    @Override
    public StarTowerInteractResp interact(StarTowerInteractReq req, StarTowerInteractResp rsp) {
        return rsp;
    }
    
    // Proto
    
    @Override
    public void encodeProto(StarTowerRoomCase proto) {
        // Set case info
        proto.getMutableNpcRecoveryHPCase()
            .setEffectId(this.getEffectId());
    }
}
