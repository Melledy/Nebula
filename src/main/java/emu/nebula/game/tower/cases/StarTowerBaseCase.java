package emu.nebula.game.tower.cases;

import emu.nebula.game.player.Player;
import emu.nebula.game.tower.StarTowerGame;
import emu.nebula.game.tower.StarTowerModifiers;
import emu.nebula.game.tower.room.StarTowerBaseRoom;
import emu.nebula.proto.PublicStarTower.StarTowerRoomCase;
import emu.nebula.proto.StarTowerInteract.StarTowerInteractReq;
import emu.nebula.proto.StarTowerInteract.StarTowerInteractResp;
import lombok.Getter;

/**
 * Base class for star tower cases
 */
@Getter
public abstract class StarTowerBaseCase {
    private transient StarTowerGame game;
    private int id;
    
    public StarTowerBaseCase() {

    }
    
    public Player getPlayer() {
        return this.getGame().getPlayer();
    }
    
    public StarTowerBaseRoom getRoom() {
        return this.getGame().getRoom();
    }
    
    public StarTowerModifiers getModifiers() {
        return this.getGame().getModifiers();
    }
    
    public abstract CaseType getType();
    
    public boolean removeAfterInteract() {
        return true;
    }

    public void register(StarTowerBaseRoom room) {
        this.game = room.getGame();
        this.id = room.getNextCaseId();
        this.onRegister();
    }
    
    public void onRegister() {
        
    }
    
    public abstract StarTowerInteractResp interact(StarTowerInteractReq req, StarTowerInteractResp rsp);
    
    // Proto
    
    public StarTowerRoomCase toProto() {
        var proto = StarTowerRoomCase.newInstance()
                .setId(this.getId());
        
        this.encodeProto(proto);
        
        return proto;
    }
    
    public abstract void encodeProto(StarTowerRoomCase proto);
}
