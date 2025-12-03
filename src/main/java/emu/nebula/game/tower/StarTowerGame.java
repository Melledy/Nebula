package emu.nebula.game.tower;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import dev.morphia.annotations.Entity;

import emu.nebula.GameConstants;
import emu.nebula.data.GameData;
import emu.nebula.data.resources.StarTowerDef;
import emu.nebula.data.resources.StarTowerStageDef;
import emu.nebula.game.formation.Formation;
import emu.nebula.game.inventory.ItemParamMap;
import emu.nebula.game.player.Player;
import emu.nebula.game.player.PlayerChangeInfo;
import emu.nebula.game.tower.cases.StarTowerBaseCase;
import emu.nebula.game.tower.cases.StarTowerDoorCase;
import emu.nebula.game.tower.cases.StarTowerPotentialCase;
import emu.nebula.game.tower.room.RoomType;
import emu.nebula.game.tower.room.StarTowerBaseRoom;
import emu.nebula.game.tower.room.StarTowerBattleRoom;
import emu.nebula.game.tower.room.StarTowerEventRoom;
import emu.nebula.game.tower.room.StarTowerHawkerRoom;
import emu.nebula.proto.PublicStarTower.*;
import emu.nebula.proto.StarTowerApply.StarTowerApplyReq;
import emu.nebula.proto.StarTowerInteract.StarTowerInteractReq;
import emu.nebula.proto.StarTowerInteract.StarTowerInteractResp;
import emu.nebula.util.Snowflake;
import emu.nebula.util.Utils;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

import lombok.Getter;
import lombok.SneakyThrows;
import us.hebi.quickbuf.RepeatedMessage;

@Getter
@Entity(useDiscriminator = false)
public class StarTowerGame {
    private transient StarTowerManager manager;
    private transient StarTowerDef data;
    
    // Tower id
    private int id;
    
    // Tower floor count
    private int floorCount;
    private int stageNum;
    private int stageFloor;
    
    // Tower room
    private StarTowerBaseRoom room;
    
    // Team
    private int formationId;
    private int buildId;
    private int teamLevel;
    private int teamExp;
    private int nextLevelExp;
    private int charHp;
    private int battleTime;
    private List<StarTowerChar> chars;
    private List<StarTowerDisc> discs;
    private IntList charIds;
    
    private int pendingPotentialCases = 0;
    private int pendingSubNotes = 0;
    
    // Bag
    private ItemParamMap items;
    private ItemParamMap res;
    private ItemParamMap potentials;
    
    // Sub note skill drop list
    private IntList subNoteDropList;
    
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
        // Set manager and cache resource data
        this.manager = manager;
        this.data = data;
        
        // Set tower id
        this.id = req.getId();
        
        // Setup room
        this.enterNextRoom();
        this.getRoom().setMapInfo(req);
        
        // Setup team
        this.formationId = req.getFormationId();
        this.buildId = Snowflake.newUid();
        this.teamLevel = 1;
        this.teamExp = 0;
        this.nextLevelExp = GameData.getStarTowerTeamExpDataTable().get(2).getNeedExp();
        this.charHp = -1;
        this.chars = new ArrayList<>();
        this.discs = new ArrayList<>();
        this.charIds = new IntArrayList();
        
        this.items = new ItemParamMap();
        this.res = new ItemParamMap();
        this.potentials = new ItemParamMap();
        this.newInfos = new ItemParamMap();
        
        // Melody skill drop list
        this.subNoteDropList = new IntArrayList();
        
        // Init formation
        for (int i = 0; i < 3; i++) {
            int id = formation.getCharIdAt(i);
            var character = getPlayer().getCharacters().getCharacterById(id);
            
            if (character != null) {
                // Add to character id list
                this.charIds.add(id);
                
                // Add sub note skill id to drop list
                int subNoteSkill = character.getData().getElementType().getSubNoteSkillItemId();
                if (subNoteSkill > 0 && !this.subNoteDropList.contains(subNoteSkill)) {
                    this.subNoteDropList.add(subNoteSkill);
                }

                this.chars.add(character.toStarTowerProto());
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
        
        // Finish setting up droppable sub note skills
        for (int id : COMMON_SUB_NOTE_SKILLS) {
            this.subNoteDropList.add(id);
        }
        
        // Starting gold
        int money = this.getStartingGold();
        if (money > 0) {
            this.getRes().add(GameConstants.STAR_TOWER_GOLD_ITEM_ID, money);
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
    
    public int getRandomCharId() {
        return Utils.randomElement(this.getCharIds());
    }
    
    public StarTowerStageDef getStageData(int stageNum, int stageFloor) {
        var stageId = (this.getId() * 10000) + (stageNum * 100) + stageFloor;
        return GameData.getStarTowerStageDataTable().get(stageId);
    }
    
    public StarTowerStageDef getNextStageData() {
        int stage = this.getStageNum();
        int floor = this.getStageFloor() + 1;
        
        if (floor >= this.getData().getMaxFloor(this.getStageNum())) {
            floor = 1;
            stage++;
        }
        
        return getStageData(stage, floor);
    }
    
    public boolean isOnFinalFloor() {
        int nextFloor = this.getFloorCount() + 1;
        return nextFloor > this.getData().getMaxFloors();
    }
    
    @SneakyThrows
    public void enterNextRoom() {
        // Increment total floor count
        this.floorCount++;
        
        // Calculate stage num/floor
        int nextStageFloor = this.stageFloor + 1;
        
        if (this.stageFloor >= this.getData().getMaxFloor(this.stageNum)) {
            this.stageNum++;
            this.stageFloor = 1;
        } else {
            this.stageFloor = nextStageFloor;
        }
        
        // Get stage data
        var stage = this.getStageData(this.stageNum, this.stageFloor);
        if (stage == null) {
            throw new RuntimeException("No stage data for stage " + this.stageNum + "-" + this.stageFloor + " found");
        }
        
        // Create room
        int roomType = stage.getRoomType();
        
        if (roomType <= RoomType.FinalBossRoom.getValue()) {
            this.room = new StarTowerBattleRoom(this, stage);
        } else if (roomType == RoomType.EventRoom.getValue()) {
            this.room = new StarTowerEventRoom(this, stage);
        } else if (roomType == RoomType.ShopRoom.getValue()) {
            this.room = new StarTowerHawkerRoom(this, stage);
        } else {
            this.room = new StarTowerBaseRoom(this, stage);
        }
        
        // Create cases for the room
        this.room.onEnter();
    }
    
    public int getStartingGold() {
        int gold = 0;
        
        if (this.getManager().hasGrowthNode(10103)) {
            gold += 50;
        } if (this.getManager().hasGrowthNode(10403)) {
            gold += 100;
        } if (this.getManager().hasGrowthNode(10702)) {
            gold += 200;
        }
        
        return gold;
    }
    
    public void addExp(int amount) {
        this.teamExp += amount;
    }

    public int levelUp(int picks) {
        if (this.teamExp >= this.nextLevelExp && this.nextLevelExp != Integer.MAX_VALUE) {
            // Level up
            this.teamLevel++;

            // Add 1 to pending potential picks
            picks++;

            // Subtract target exp
            this.teamExp = this.teamExp - this.nextLevelExp;

            // Update next level exp
            var teamExpData = GameData.getStarTowerTeamExpDataTable().get(this.teamLevel + 1);
            if (teamExpData != null) {
                this.nextLevelExp = teamExpData.getNeedExp();
            } else {
                this.nextLevelExp = Integer.MAX_VALUE;
            }
            
            // Re-check and continue processing if we still got exp
            if (this.teamExp > 0) {
                return levelUp(picks);
            }
        }

        // Return picks
        return picks;
    }

    public int levelUp() {
        int potentialPicks = 0;
        return this.levelUp(potentialPicks);
    }
    
    public void addBattleTime(int amount) {
        this.battleTime += amount;
    }
    
    // Cases
    
    public StarTowerBaseCase addCase(StarTowerBaseCase towerCase) {
        return this.getRoom().addCase(towerCase);
    }
    
    public StarTowerBaseCase addCase(RepeatedMessage<StarTowerRoomCase> cases, StarTowerBaseCase towerCase) {
        return this.getRoom().addCase(cases, towerCase);
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
                
                // Add to change info
                var info = PotentialInfo.newInstance()
                        .setTid(id)
                        .setLevel(count);
                
                change.add(info);
            }
            case SubNoteSkill -> {
                // Add to items
                this.getItems().add(id, count);
                
                // Add to change info
                var info = TowerItemInfo.newInstance()
                        .setTid(id)
                        .setQty(count);
                
                change.add(info);
                
                // Add to new infos
                this.getNewInfos().add(id, count);
            }
            case Res -> {
                // Add to res
                this.getRes().add(id, count);
                
                // Add to change info
                var info = TowerResInfo.newInstance()
                        .setTid(id)
                        .setQty(count);
                
                change.add(info);
            }
            default -> {
                // Ignored
            }
        }
        
        // Return changes
        return change;
    }
    
    // Potentials/Sub notes
    
    /**
     * Adds random potential selector cases for the client
     */
    public void addPotentialSelectors(int amount) {
        this.pendingPotentialCases += amount;
    }
    
    /**
     * Creates a potential selector for the client if there are any potential selectors avaliable.
     * If there are none, then return null.
     */
    public StarTowerBaseCase handlePendingPotentialSelectors() {
        if (this.pendingPotentialCases > 0) {
            this.pendingPotentialCases--;
            return this.createPotentialSelector();
        } else {
            return null;
        }
    }
    
    /**
     * Creates a potential selector for a random character
     */
    public StarTowerBaseCase createPotentialSelector() {
        int charId = this.getRandomCharId();
        return this.createPotentialSelector(charId);
    }
    
    /**
     * Creates a potential selector for the specified character
     */
    public StarTowerBaseCase createPotentialSelector(int charId) {
        // Get character potentials
        var data = GameData.getCharPotentialDataTable().get(charId);
        if (data == null) {
            return null;
        }
        
        // Random potentials list
        var potentials = new IntArrayList();
        
        // Add potentials based on character role
        boolean isMainCharacter = this.getCharIds().getInt(0) == charId;
        
        if (isMainCharacter) {
            potentials.addElements(0, data.getMasterSpecificPotentialIds());
            potentials.addElements(0, data.getMasterNormalPotentialIds());
        } else {
            potentials.addElements(0, data.getAssistSpecificPotentialIds());
            potentials.addElements(0, data.getAssistNormalPotentialIds());
        }
        
        potentials.addElements(0, data.getCommonPotentialIds());
        
        // Get up to 3 random potentials
        IntList selector = new IntArrayList();
        
        for (int i = 0; i < 3; i++) {
            // Sanity check
            if (potentials.isEmpty()) {
                break;
            }
            
            // Get random potential id
            int index = ThreadLocalRandom.current().nextInt(0, potentials.size());
            int potentialId = potentials.getInt(index);
            
            // Add to selector
            selector.add(potentialId);
            
            // Remove potential id from the selector
            potentials.removeInt(index);
        }
        
        // Sanity check
        if (selector.isEmpty()) {
            return null;
        }
        
        // Creator potential selector case
        return new StarTowerPotentialCase(this.getTeamLevel(), selector);
    }
    
    public void setPendingSubNotes(int amount) {
        this.pendingSubNotes = amount;
    }
    
    private PlayerChangeInfo addRandomSubNoteSkills(PlayerChangeInfo change) {
        int count = Utils.randomRange(1, 3);
        int id = Utils.randomElement(this.getSubNoteDropList());
        
        this.addItem(id, count, change);
        
        return change;
    }
    
    public PlayerChangeInfo addRandomSubNoteSkills(int count, PlayerChangeInfo change) {
        for (int i = 0; i < count; i++) {
            this.addRandomSubNoteSkills(change);
        }
        
        return change;
    }
    
    // Door case
    
    public StarTowerBaseCase createExit() {
        return this.createExit(null);
    }
    
    public StarTowerBaseCase createExit(RepeatedMessage<StarTowerRoomCase> cases) {
        return this.getRoom().addCase(
            cases,
            new StarTowerDoorCase(this.getFloorCount() + 1, this.getNextStageData())
        );
    }
    
    // Handlers
    
    public StarTowerInteractResp handleInteract(StarTowerInteractReq req) {
        // Build response proto
        var rsp = StarTowerInteractResp.newInstance()
                .setId(req.getId());
        
        // Get tower case
        var towerCase = this.getRoom().getCase(req.getId());
        
        // Handle interaction with tower case
        if (towerCase != null) {
            rsp = towerCase.interact(req, rsp);
        } else {
            rsp.getMutableNilResp();
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
        
        // Return response proto
        return rsp;
    }
    
    public StarTowerInteractResp settle(StarTowerInteractResp rsp, boolean isWin) {
        // End game
        this.getManager().endGame(isWin);
        
        // Settle info
        var settle = rsp.getMutableSettle()
                .setTotalTime(this.getBattleTime())
                .setBuild(this.getBuild().toProto());
        
        // Mark change info
        settle.getMutableChange();
        
        // Log victory
        if (isWin) {
            this.getManager().getPlayer().getProgress().addStarTowerLog(this.getId());
        }
        
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
        
        // Set room data
        proto.setRoom(this.getRoom().toProto());
        
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
        
        for (var entry : this.getRes()) {
            var res = TowerResInfo.newInstance()
                    .setTid(entry.getIntKey())
                    .setQty(entry.getIntValue());
            
            bag.addRes(res);
        }
        
        return proto;
    }

}
