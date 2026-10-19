package emu.nebula.game.tracehunt;

import emu.nebula.Nebula;
import emu.nebula.data.GameData;
import emu.nebula.game.GameContext;
import emu.nebula.game.GameContextModule;

public class TraceHuntModule extends GameContextModule {
    private int controlId;
    
    public TraceHuntModule(GameContext context) {
        super(context);
        this.updateControl();
    }
    
    public int getControlId() {
        return this.controlId;
    }
    
    public void updateControl() {
        int control = 1;
        long time = Nebula.getCurrentServerTime();
        
        for (var data : GameData.getTraceHuntControlDataTable()) {
            if (data.getId() <= control) {
                continue;
            }
            
            if (time >= data.getStartDate()) {
                control = data.getId();
            }
        }
        
        this.controlId = control;
    }
}
