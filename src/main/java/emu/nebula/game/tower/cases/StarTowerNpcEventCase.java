package emu.nebula.game.tower.cases;

import java.util.Collections;

import emu.nebula.GameConstants;
import emu.nebula.data.resources.StarTowerEventDef;
import emu.nebula.game.player.PlayerChangeInfo;
import emu.nebula.proto.PublicStarTower.NPCAffinityInfo;
import emu.nebula.proto.PublicStarTower.StarTowerRoomCase;
import emu.nebula.proto.StarTowerInteract.StarTowerInteractReq;
import emu.nebula.proto.StarTowerInteract.StarTowerInteractResp;
import emu.nebula.util.Utils;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import lombok.Getter;

@Getter
public class StarTowerNpcEventCase extends StarTowerBaseCase {
    private int npcId;
    private int eventId;
    private IntList options;
    private boolean completed;
    
    public StarTowerNpcEventCase(int npcId, StarTowerEventDef event) {
        this.npcId = npcId;
        this.eventId = event.getId();
        this.options = new IntArrayList();
        
        // Add up to 4 random options
        var randomOptions = event.getClonedOptionIds();
        int maxOptions = Math.min(randomOptions.size(), 4);
        
        for (int i = 0; i < maxOptions; i++) {
            int optionId = Utils.randomElement(randomOptions, true);
            this.options.add(optionId);
        }
        
        // Fix for question type events to always include the answer
        if (this.eventId >= 114 && this.eventId <= 116) {
            int answerId = (this.eventId * 100) + 3;
            if (!this.getOptions().contains(answerId)) {
                this.getOptions().set(0, answerId);
            }
        }
        
        // Shuffle
        Collections.shuffle(this.getOptions());
    }

    @Override
    public CaseType getType() {
        return CaseType.NpcEvent;
    }
    
    public int getOption(int index) {
        if (index < 0 || index >= this.getOptions().size()) {
            return 0;
        }
        
        return this.getOptions().getInt(index);
    }
    
    @Override
    public StarTowerInteractResp interact(StarTowerInteractReq req, StarTowerInteractResp rsp) {
        // Sanity check to make sure we cant do the event multiple times
        if (this.isCompleted()) {
            return rsp;
        }
        
        // Get option from selection index
        int option = this.getOption(req.getSelectReq().getIndex());
        
        // Get select response proto
        var selectRsp = rsp.getMutableSelectResp();
        var success = selectRsp.getMutableResp();
        var change = new PlayerChangeInfo();
        
        // Completed event flag
        boolean completed = true;
        
        // Handle option id
        switch (option) {
            case 10101 -> {
                if (this.spendCoin(100, change)) {
                    this.addPotentialSelector(rsp);
                } else {
                    completed = false;
                }
            }
            case 10102 -> {
                if (this.spendCoin(120, change)) {
                    this.addPotentialSelector(rsp);
                } else {
                    completed = false;
                }
            }
            case 10103 -> {
                this.addCoin(30, change);
            }
            case 10201 -> {
                if (this.spendCoin(120, change)) {
                    this.addPotentialSelector(rsp, this.getRandomSupportCharId());
                } else {
                    completed = false;
                }
            }
            case 10202 -> {
                if (this.spendCoin(160, change)) {
                    this.addPotentialSelector(rsp, this.getMainCharId());
                } else {
                    completed = false;
                }
            }
            case 10203 -> {
                if (this.spendCoin(200, change)) {
                    this.addRarePotentialSelector(rsp);
                } else {
                    completed = false;
                }
            }
            case 10204 -> {
                this.addCoin(30, change);
            }
            case 10302 -> {
                // TODO
                if (this.spendSubNotes(5, change)) {
                    this.addCoin(150, change);
                } else {
                    completed = false;
                }
            }
            case 10303 -> {
                this.addCoin(30, change);
            }
            case 10401 -> {
                // TODO
                completed = false;
            }
            case 10402 -> {
                if (this.spendCoin(200, change)) {
                    this.addRarePotentialSelector(rsp);
                } else {
                    completed = false;
                }
            }
            case 10403 -> {
                this.addCoin(30, change);
            }
            case 10501 -> {
                if (Utils.randomChance(.5)) {
                    this.addCoin(200, change);
                } else {
                    this.addCoin(-100, change);
                }
            }
            case 10502 -> {
                if (Utils.randomChance(.3)) {
                    this.addCoin(650, change);
                } else {
                    this.addCoin(-200, change);
                }
            }
            case 10503 -> {
                this.addCoin(30, change);
            }
            case 10601 -> {
                if (Utils.randomChance(.5)) {
                    this.addRarePotentialSelector(rsp);
                }
            }
            case 10602 -> {
                this.addPotentialSelector(rsp);
            }
            case 10603 -> {
                this.addCoin(30, change);
            }
            case 10701, 10702, 10703, 10704, 10705, 10706, 10707 -> {
                int subNoteId = (option % 100) + 90010;
                this.getGame().addItem(subNoteId, 5, change);
            }
            case 10708 -> {
                int subNoteId = this.getGame().getRandomSubNoteId();
                this.getGame().addItem(subNoteId, 5, change); 
            }
            case 10801, 10802, 10803, 10804, 10805, 10806, 10807 -> {
                if (this.spendCoin(140, change)) {
                    int subNoteId = (option % 100) + 90010;
                    this.getGame().addItem(subNoteId, 10, change);
                } else {
                    completed = false;
                }
            }
            case 10808 -> {
                if (this.spendCoin(90, change)) {
                    int subNoteId = this.getGame().getRandomSubNoteId();
                    this.getGame().addItem(subNoteId, 10, change);
                } else {
                    completed = false;
                }
            }
            case 10809 -> {
                this.addCoin(30, change);
            }
            case 11401, 11402, 11403, 11404, 11405 -> {
                if (option == 11403) {
                    int subNoteId = this.getGame().getRandomSubNoteId();
                    this.getGame().addItem(subNoteId, 10, change);
                } else {
                    success.setOptionsParamId(100140101);
                }
            }
            case 11501, 11502, 11503, 11504, 11505 -> {
                if (option == 11503) {
                    this.addPotentialSelector(rsp);
                } else {
                    success.setOptionsParamId(100140101);
                }
            }
            case 11601, 11602, 11603, 11604, 11605 -> {
                if (option == 11603) {
                    this.addRarePotentialSelector(rsp);
                } else {
                    success.setOptionsParamId(100140101);
                }
            }
            case 12601 -> {
                this.addPotentialSelector(rsp, this.getRandomSupportCharId());
            }
            case 12602 -> {
                // Recover 20% hp
            }
            case 12701 -> {
                this.addPotentialSelector(rsp, this.getRandomSupportCharId());
            }
            case 12702 -> {
                int subNoteId = this.getGame().getRandomSubNoteId();
                this.getGame().addItem(subNoteId, 5, change);
            }
            case 12801 -> {
                this.addRarePotentialSelector(rsp, this.getRandomSupportCharId());
            }
            case 12802 -> {
                this.addCoin(30, change);
            }
            default -> {
                // Ignored
            }
        }
        
        // Set change info
        rsp.setChange(change.toProto());
        
        // Set success result
        success.setOptionsResult(completed);
        this.completed = completed;
        
        // Complete
        return rsp;
    }

    // Helper functions
    
    private boolean spendCoin(int amount, PlayerChangeInfo change) {
        int coin = this.getGame().getResCount(GameConstants.TOWER_COIN_ITEM_ID);
        
        if (coin < amount) {
            return false;
        }
        
        this.addCoin(-amount, change);
        
        return true;
    }
    
    private PlayerChangeInfo addCoin(int amount, PlayerChangeInfo change) {
        return this.getGame().addItem(GameConstants.TOWER_COIN_ITEM_ID, amount, change);
    }
    
    private boolean spendSubNotes(int amount, PlayerChangeInfo change) {
        // TODO
        return false;
    }
    
    private void addPotentialSelector(StarTowerInteractResp rsp) {
        this.addPotentialSelector(rsp, 0);
    }
    
    private void addPotentialSelector(StarTowerInteractResp rsp, int charId) {
        var selectorCase = this.getGame().createPotentialSelector(charId);
        this.getRoom().addCase(rsp.getMutableCases(), selectorCase);
    }
    
    private void addRarePotentialSelector(StarTowerInteractResp rsp) {
        this.addRarePotentialSelector(rsp, 0);
    }
    
    private void addRarePotentialSelector(StarTowerInteractResp rsp, int charId) {
        var selectorCase = this.getGame().createPotentialSelector(charId, true);
        this.getRoom().addCase(rsp.getMutableCases(), selectorCase);
    }
    
    private int getMainCharId() {
        return this.getGame().getCharIds()[0];
    }
    
    private int getRandomSupportCharId() {
        return this.getGame().getCharIds()[Utils.randomRange(1, 2)];
    }
    
    // Proto
    
    @Override
    public void encodeProto(StarTowerRoomCase proto) {
        var info = NPCAffinityInfo.newInstance()
            .setNPCId(this.getNpcId())
            .setAffinity(0);
        
        proto.getMutableSelectOptionsEventCase()
            .setEvtId(this.getEventId())
            .setNPCId(this.getNpcId())
            .addInfos(info)
            .addAllOptions(this.getOptions().toIntArray());
    }
}
