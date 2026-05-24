package emu.nebula.game.player;

import lombok.Getter;

@Getter
public abstract class PlayerManager {
    private transient Player player;

    public PlayerManager() {
        
    }
    
    public PlayerManager(Player player) {
        this.player = player;
    }

    public void setPlayer(Player player) {
        if (this.player == null) {
            this.player = player;
        }
    }

    public int getPlayerUid() {
        return this.getPlayer().getUid();
    }
    
    /**
     * Called when the manager is loaded from the database
     */
    public void onLoad() {
        
    }
}
