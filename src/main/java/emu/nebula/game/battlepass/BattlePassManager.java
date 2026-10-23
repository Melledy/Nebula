package emu.nebula.game.battlepass;

import emu.nebula.GameConstants;
import emu.nebula.Nebula;
import emu.nebula.game.player.Player;
import emu.nebula.game.player.PlayerManager;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.Public.BattlePassState;

import lombok.Getter;

@Getter
public class BattlePassManager extends PlayerManager {
    private BattlePass battlePass;

    public BattlePassManager(Player player) {
        super(player);
    }
    
    public boolean isUnlocked() {
        return this.getPlayer().getLevel() >= GameConstants.BATTLE_PASS_UNLOCK_LEVEL;
    }
    
    /**
     * Updates the battle pass state on the client
     */
    public void updateState() {
        this.getPlayer().addNextPackage(
            NetMsgId.battle_pass_state_notify,
            this.getStateProto()
        );
    }

    /**
     * Pushes a full battle-pass snapshot for clients that only refresh their
     * local battle-pass cache/red dots from the page-info payload.
     */
    public void queueBattlePassInfoNotify() {
        this.getPlayer().addNextPackage(
            NetMsgId.battle_pass_info_succeed_ack,
            this.getBattlePass().toProto()
        );
    }

    /**
     * Battle-pass unlock is a special case: clients need the full page snapshot
     * to initialize local battle-pass cache and refresh the main-entry red dot.
     */
    public void queueUnlockNotify(int oldLevel) {
        if (oldLevel < GameConstants.BATTLE_PASS_UNLOCK_LEVEL && this.isUnlocked()) {
            this.queueBattlePassInfoNotify();
        }
    }
    
    // Proto
    
    public BattlePassState getStateProto() {
        int state = this.isUnlocked()
                ? this.getBattlePass().getClientState()
                : 0;

        return BattlePassState.newInstance().setState(state);
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
