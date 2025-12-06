package emu.nebula.game.battlepass;

import emu.nebula.Nebula;
import emu.nebula.game.player.Player;
import emu.nebula.game.player.PlayerManager;
import lombok.Getter;

@Getter
public class BattlePassManager extends PlayerManager {
    private BattlePass battlePass;

    public BattlePassManager(Player player) {
        super(player);
    }

    public boolean hasNew() {
        return this.getBattlePass().hasNew();
    }
    
    // Database
    
    public void loadFromDatabase() {
        this.battlePass = Nebula.getGameDatabase().getObjectByUid(BattlePass.class, getPlayer().getUid());
        
        if (this.battlePass == null) {
            this.battlePass = new BattlePass(this);
        } else {
            this.battlePass.setManager(this);
        }
    }
}
