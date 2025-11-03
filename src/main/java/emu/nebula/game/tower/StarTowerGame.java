package emu.nebula.game.tower;

import java.util.ArrayList;
import java.util.List;

import dev.morphia.annotations.Entity;
import emu.nebula.data.GameData;
import emu.nebula.data.resources.PotentialDef;
import emu.nebula.data.resources.StarTowerDef;
import emu.nebula.data.resources.StarTowerStageDef;
import emu.nebula.game.formation.Formation;
import emu.nebula.game.inventory.ItemParamMap;
import emu.nebula.game.player.Player;
import emu.nebula.game.player.PlayerChangeInfo;
import emu.nebula.proto.PublicStarTower.*;
import emu.nebula.proto.StarTowerApply.StarTowerApplyReq;
import emu.nebula.proto.StarTowerInteract.StarTowerInteractReq;
import emu.nebula.proto.StarTowerInteract.StarTowerInteractResp;
import emu.nebula.util.Snowflake;
import emu.nebula.util.Utils;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

import lombok.Getter;
import lombok.SneakyThrows;

@Getter
@Entity(useDiscriminator = false)
public class StarTowerGame {
    private transient StarTowerManager manager;
    private transient StarTowerDef data;
    
    // Tower id
    private int id;
    
    // Room
    private int stageNum;
    private int stageFloor;
    private int floor;
    private int mapId;
    private int mapTableId;
    private String mapParam;
    private int paramId;
    private int roomType;
    
    // Team
    private int formationId;
    private int buildId;
    private int teamLevel;
    private int teamExp;
    private int charHp;
    private int battleTime;
    private int battleCount;
    private List<StarTowerChar> chars;
    private List<StarTowerDisc> discs;
    private IntSet charIds;
    
    // Case
    private int lastCaseId = 0;
    private int selectorCaseIndex = -1;
    private int battleCaseIndex = -1;
    private List<StarTowerCase> cases;
    
    // Bag
    private ItemParamMap items;
    private ItemParamMap res;
    private ItemParamMap potentials;
    
    // Cached build
    private transient StarTowerBuild build;
    private transient ItemParamMap newInfos;
    
    private static final int[] COMMON_SUB_NOTE_SKILLS = new int[] {
        90011, 90012, 90013, 90014, 90015, 90016, 90017
    };
    
    @Deprecated // Morphia only
    public StarTowerGame() {
        
    }
    
    public StarTowerGame(StarTowerManager manager, StarTowerDef data, Formation formation, StarTowerApplyReq req) {
        this.manager = manager;
        this.data = data;
        
        this.id = req.getId();
        
        this.mapId = req.getMapId();
        this.mapTableId = req.getMapTableId();
        this.mapParam = req.getMapParam();
        this.paramId = req.getParamId();
        
        this.formationId = req.getFormationId();
        this.buildId = Snowflake.newUid();
        this.teamLevel = 1;
        this.stageNum = 1;
        this.stageFloor = 1;
        this.floor = 1;
        this.charHp = -1;
        this.chars = new ArrayList<>();
        this.discs = new ArrayList<>();
        this.charIds = new IntOpenHashSet();

        this.cases = new ArrayList<>();
        
        this.items = new ItemParamMap();
        this.res = new ItemParamMap();
        this.potentials = new ItemParamMap();
        this.newInfos = new ItemParamMap();
        
        // Init formation
        for (int i = 0; i < 3; i++) {
            int id = formation.getCharIdAt(i);
            var character = getPlayer().getCharacters().getCharacterById(id);
            
            if (character != null) {
                this.chars.add(character.toStarTowerProto());
                this.charIds.add(id);
            } else {
                this.chars.add(StarTowerChar.newInstance());
            }
        }
        
        for (int i = 0; i < 6; i++) {
            int id = formation.getDiscIdAt(i);
            var disc = getPlayer().getCharacters().getDiscById(id);
            
            if (disc != null) {
                this.discs.add(disc.toStarTowerProto());
                
                // Add star tower sub note skills
                if (i >= 3) {
                    var subNoteData = disc.getSubNoteSkillDef();
                    if (subNoteData != null) {
                        this.getItems().add(subNoteData.getItems());
                    }
                }
            } else {
                this.discs.add(StarTowerDisc.newInstance());
            }
        }
        
        // Add cases
        this.addCase(new StarTowerCase(CaseType.Battle));
        this.addCase(new StarTowerCase(CaseType.SyncHP));
        
        // Debug
        var doorCase = this.addCase(new StarTowerCase(CaseType.OpenDoor));
        doorCase.setFloorId(this.getStageFloor() + 1);
        
        var nextStage = this.getNextStageData();
        if (nextStage != null) {
            doorCase.setRoomType(nextStage.getRoomType());
        }
    }
    
    public Player getPlayer() {
        return this.manager.getPlayer();
    }
    
    public StarTowerBuild getBuild() {
        if (this.build == null) {
            this.build = new StarTowerBuild(this);
        }
        
        return this.build;
    }
    
    public StarTowerStageDef getStageData(int stage, int floor) {
        var stageId = (this.getId() * 10000) + (stage * 100) + floor;
        return GameData.getStarTowerStageDataTable().get(stageId);
    }
    
    public StarTowerStageDef getNextStageData() {
        int stage = this.stageNum;
        int floor = this.stageFloor + 1;
        
        if (floor >= this.getData().getMaxFloor(this.getStageNum())) {
            floor = 1;
            stage++;
        }
        
        return getStageData(stage, floor);
    }
    
    // Cases
    
    public StarTowerCase getBattleCase() {
        if (this.getBattleCaseIndex() < 0 || this.getBattleCaseIndex() >= this.getCases().size()) {
            return null;
        }
        
        return this.getCases().get(this.getBattleCaseIndex());
    }
    
    public StarTowerCase getSelectorCase() {
        if (this.getSelectorCaseIndex() < 0 || this.getSelectorCaseIndex() >= this.getCases().size()) {
            return null;
        }
        
        return this.getCases().get(this.getSelectorCaseIndex());
    }
    
    public StarTowerCase addCase(StarTowerCase towerCase) {
        return this.addCase(null, towerCase);
    }
    
    public StarTowerCase addCase(StarTowerInteractResp rsp, StarTowerCase towerCase) {
        // Add to cases list
        this.getCases().add(towerCase);
        
        // Increment id
        towerCase.setId(++this.lastCaseId);
        
        // Set proto
        if (rsp != null) {
            rsp.getMutableCases().add(towerCase.toProto());
        }
        
        //
        if (towerCase.getIds() != null) {
            this.selectorCaseIndex = this.getCases().size() - 1;
        } else if (towerCase.getType() == CaseType.Battle) {
            this.battleCaseIndex = this.getCases().size() - 1;
        }
        
        return towerCase;
    }
    
    // Items

    public int getItemCount(int id) {
        return this.getItems().get(id);
    }
    
    public PlayerChangeInfo addItem(int id, int count, PlayerChangeInfo change) {
        // Create changes if null
        if (change == null) {
            change = new PlayerChangeInfo();
        }
        
        // Get item data
        var itemData = GameData.getItemDataTable().get(id);
        if (itemData == null) {
            return change;
        }
        
        // Handle changes
        switch (itemData.getItemSubType()) {
            case Potential, SpecificPotential -> {
                // Get potential data
                var potentialData = GameData.getPotentialDataTable().get(id);
                if (potentialData == null) return change;
                
                // Clamp level
                int curLevel = getPotentials().get(id);
                int nextLevel = Math.min(curLevel + count, potentialData.getMaxLevel());
                
                // Sanity
                count = nextLevel - curLevel;
                if (count <= 0) {
                    return change;
                }
                
                // Add potential
                this.getPotentials().put(id, nextLevel);
                
                // Add change
                var info = PotentialInfo.newInstance()
                        .setTid(id)
                        .setLevel(count);
                
                change.add(info);
            }
            case SubNoteSkill -> {
                // Add to items
                this.getItems().add(id, count);
                
                // Add change
                var info = TowerItemInfo.newInstance()
                        .setTid(id)
                        .setQty(count);
                
                change.add(info);
                
                // Add to new infos
                this.getNewInfos().add(id, count);
            }
            default -> {
                // Ignored
            }
        }
        
        // Return changes
        return change;
    }
    
    // Potentials/Sub notes
    
    private StarTowerCase createPotentialSelector(int charId) {
        // Add potential selector
        var potentialCase = new StarTowerCase(CaseType.SelectSpecialPotential);
        potentialCase.setTeamLevel(this.getTeamLevel());
        
        // Get random potentials
        List<PotentialDef> potentials = new ArrayList<>();
        
        for (var potentialData : GameData.getPotentialDataTable()) {
            if (potentialData.getCharId() == charId) {
                potentials.add(potentialData);
            }
        }
        
        for (int i = 0; i < 3; i++) {
            var potentialData = Utils.randomElement(potentials);
            potentialCase.addId(potentialData.getId());
        }
        
        return potentialCase;
    }
    
    private PlayerChangeInfo addRandomSubNoteSkills(PlayerChangeInfo change) {
        int count = Utils.randomRange(1, 3);
        int id = Utils.randomElement(COMMON_SUB_NOTE_SKILLS);
        
        this.addItem(id, count, change);
        
        return change;
    }
    
    private PlayerChangeInfo addRandomSubNoteSkills(int count, PlayerChangeInfo change) {
        for (int i = 0; i < count; i++) {
            this.addRandomSubNoteSkills(change);
        }
        
        return change;
    }
    
    // Handlers
    
    public StarTowerInteractResp handleInteract(StarTowerInteractReq req) {
        var rsp = StarTowerInteractResp.newInstance()
                .setId(req.getId());
                
        if (req.hasBattleEndReq()) {
            rsp = this.onBattleEnd(req, rsp);
        } else if (req.hasRecoveryHPReq()) {
            rsp = this.onRecoveryHP(req, rsp);
        } else if (req.hasSelectReq()) {
            rsp = this.onSelect(req, rsp);
        } else if (req.hasEnterReq()) {
            rsp = this.onEnterReq(req, rsp);
        }
        
        // Add any items
        var data = rsp.getMutableData();
        
        if (this.getNewInfos().size() > 0) {
            // Add item protos
            for (var entry : this.getNewInfos()) {
                var info = SubNoteSkillInfo.newInstance()
                        .setTid(entry.getIntKey())
                        .setQty(entry.getIntValue());
                
                data.getMutableInfos().add(info);
            }
            
            // Clear
            this.getNewInfos().clear();
        }
        
        // Set these protos
        rsp.getMutableChange();
        
        return rsp;
    }
    
    // Interact events
    
    @SneakyThrows
    public StarTowerInteractResp onBattleEnd(StarTowerInteractReq req, StarTowerInteractResp rsp) {
        var proto = req.getBattleEndReq();
        
        if (proto.hasVictory()) {
            // Add team level
            this.teamLevel++;
            
            // Add clear time
            this.battleTime += proto.getVictory().getTime();
            
            // Handle victory
            rsp.getMutableBattleEndResp()
                .getMutableVictory()
                .setLv(this.getTeamLevel())
                .setBattleTime(this.getBattleTime());
            
            // Create potential selector
            int charId = this.getChars().get(battleCount % this.getCharIds().size()).getId();
            var potentialCase = this.createPotentialSelector(charId);
            
            // Add case
            this.addCase(rsp, potentialCase);
            
            // Add sub note skills
            var battleCase = this.getBattleCase();
            if (battleCase != null) {
                var change = new PlayerChangeInfo();
                int subNoteSkills = battleCase.getSubNoteSkillNum();
                
                this.addRandomSubNoteSkills(subNoteSkills, change);
                
                rsp.setChange(change.toProto());
            }
        } else {
            // Handle defeat
            // TODO
        }
        
        // Increment battle count
        this.battleCount++;
        
        return rsp;
    }

    public StarTowerInteractResp onSelect(StarTowerInteractReq req, StarTowerInteractResp rsp) {
        var index = req.getMutableSelectReq().getIndex();
        
        var selectorCase = this.getSelectorCase();
        if (selectorCase == null) {
            return rsp;
        }
        
        int id = selectorCase.selectId(index);
        if (id <= 0) {
            return rsp;
        }
        
        // Add item
        var change = this.addItem(id, 1, null);
        
        // Set change
        rsp.setChange(change.toProto());
        
        return rsp;
    }
    
    public StarTowerInteractResp onEnterReq(StarTowerInteractReq req, StarTowerInteractResp rsp) {
        // Get proto
        var proto = req.getEnterReq();
        
        // Set
        this.mapId = proto.getMapId();
        this.mapTableId = proto.getMapTableId();
        this.mapParam = proto.getMapParam();
        this.paramId = proto.getParamId();
        this.floor++;
        
        // Check if we need to settle
        if (this.floor > this.getData().getMaxFloors()) {
            return this.settle(rsp);
        }
        
        // Next floor
        int nextFloor = this.stageFloor + 1;
        
        if (this.stageFloor >= this.getData().getMaxFloor(this.getStageNum())) {
            this.stageFloor = 1;
            this.stageNum++;
        } else {
            this.stageFloor = nextFloor;
        }
        
        // Calculate stage
        var stageData = this.getStageData(this.getStageNum(), this.getStageFloor());
        
        if (stageData != null) {
            this.roomType = stageData.getRoomType();
        } else {
            this.roomType = 0;
        }
        
        // Clear cases
        this.selectorCaseIndex = -1;
        this.battleCaseIndex = -1;
        this.lastCaseId = 0;
        this.cases.clear();
        
        // Add cases
        var syncHpCase = new StarTowerCase(CaseType.SyncHP);
        var doorCase = new StarTowerCase(CaseType.OpenDoor);
        doorCase.setFloorId(this.getFloor() + 1);
        
        // Set room type of next room
        var nextStage = this.getNextStageData();
        if (nextStage != null) {
            doorCase.setRoomType(nextStage.getRoomType());
        }
        
        // Room proto
        var room = rsp.getMutableEnterResp().getMutableRoom();
        room.setData(this.toRoomDataProto());
        
        // Handle room type TODO
        if (this.roomType <= StarTowerRoomType.FinalBossRoom.getValue()) {
            var battleCase = new StarTowerCase(CaseType.Battle);
            battleCase.setSubNoteSkillNum(Utils.randomRange(1, 3));
            
            this.addCase(battleCase);
            room.addCases(battleCase.toProto());
        }
        
        // Add cases
        this.addCase(syncHpCase);
        this.addCase(doorCase);
        
        // Add cases to room
        room.addCases(syncHpCase.toProto());
        room.addCases(doorCase.toProto());
        
        return rsp;
    }

    public StarTowerInteractResp onRecoveryHP(StarTowerInteractReq req, StarTowerInteractResp rsp) {
        // Add case
        this.addCase(rsp, new StarTowerCase(CaseType.RecoveryHP));
        
        return rsp;
    }
    
    public StarTowerInteractResp settle(StarTowerInteractResp rsp) {
        // End game
        this.getManager().endGame();
        
        // Settle info
        var settle = rsp.getMutableSettle()
                .setTotalTime(this.getBattleTime())
                .setBuild(this.getBuild().toProto());
        
        // Mark change info
        settle.getMutableChange();
        
        // Complete
        return rsp;
    }
    
    // Proto
    
    public StarTowerInfo toProto() {
        var proto = StarTowerInfo.newInstance();
        
        proto.getMutableMeta()
            .setId(this.getId())
            .setCharHp(this.getCharHp())
            .setTeamLevel(this.getTeamLevel())
            .setNPCInteractions(1)
            .setBuildId(this.getBuildId());
        
        this.getChars().forEach(proto.getMutableMeta()::addChars);
        this.getDiscs().forEach(proto.getMutableMeta()::addDiscs);
        
        proto.getMutableRoom().setData(this.toRoomDataProto());
        
        // Cases
        for (var starTowerCase : this.getCases()) {
            proto.getMutableRoom().addCases(starTowerCase.toProto());
        }
        
        // Set up bag
        var bag = proto.getMutableBag();
        
        for (var entry : this.getItems()) {
            var item = TowerItemInfo.newInstance()
                    .setTid(entry.getIntKey())
                    .setQty(entry.getIntValue());
            
            bag.addItems(item);
        }
        
        for (var entry : this.getPotentials()) {
            var item = PotentialInfo.newInstance()
                    .setTid(entry.getIntKey())
                    .setLevel(entry.getIntValue());
            
            bag.addPotentials(item);
        }
        
        return proto;
    }
    
    public StarTowerRoomData toRoomDataProto() {
        var proto = StarTowerRoomData.newInstance()
                .setFloor(this.getStageFloor())
                .setMapId(this.getMapId())
                .setRoomType(this.getRoomType())
                .setMapTableId(this.getMapTableId());
        
        if (this.getMapParam() != null && !this.getMapParam().isEmpty()) {
            proto.setMapParam(this.getMapParam());
        }
        
        if (this.getParamId() != 0) {
            proto.setParamId(this.getParamId());
        }
        
        return proto;
    }

}
