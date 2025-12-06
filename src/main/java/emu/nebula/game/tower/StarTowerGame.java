package emu.nebula.game.tower;

import java.util.ArrayList;
import java.util.List;

import dev.morphia.annotations.Entity;

import emu.nebula.GameConstants;
import emu.nebula.data.GameData;
import emu.nebula.data.resources.SecondarySkillDef;
import emu.nebula.data.resources.StarTowerDef;
import emu.nebula.data.resources.StarTowerStageDef;
import emu.nebula.game.character.GameCharacter;
import emu.nebula.game.character.GameDisc;
import emu.nebula.game.formation.Formation;
import emu.nebula.game.inventory.ItemParamMap;
import emu.nebula.game.player.Player;
import emu.nebula.game.player.PlayerChangeInfo;
import emu.nebula.game.tower.cases.StarTowerBaseCase;
import emu.nebula.game.tower.cases.StarTowerDoorCase;
import emu.nebula.game.tower.cases.StarTowerHawkerCase;
import emu.nebula.game.tower.cases.StarTowerNpcRecoveryHPCase;
import emu.nebula.game.tower.cases.StarTowerPotentialCase;
import emu.nebula.game.tower.cases.StarTowerSelectSpecialPotentialCase;
import emu.nebula.game.tower.cases.StarTowerStrengthenMachineCase;
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
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
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
    private int[] charIds;
    private int[] discIds;
    
    private int pendingPotentialCases = 0;
    private int pendingSubNotes = 0;
    
    // Bag
    private ItemParamMap items;
    private ItemParamMap res;
    private ItemParamMap potentials;
    private IntSet secondarySkills;
    
    // Sub note skill drop list
    private IntList subNoteDropList;
    
    // Modifiers
    private StarTowerModifiers modifiers;
    
    // Cached build
    private transient StarTowerBuild build;
    private transient ItemParamMap newInfos;
    
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
        
        this.items = new ItemParamMap();
        this.res = new ItemParamMap();
        this.potentials = new ItemParamMap();
        this.secondarySkills = new IntOpenHashSet();
        
        this.newInfos = new ItemParamMap();
        
        // Init melody drop list
        this.subNoteDropList = new IntArrayList();
        
        // Init modifiers
        this.modifiers = new StarTowerModifiers(this);
        
        // Init formation
        IntList charList = new IntArrayList();
        IntList discList = new IntArrayList();
        
        for (int i = 0; i < 3; i++) {
            int id = formation.getCharIdAt(i);
            var character = getPlayer().getCharacters().getCharacterById(id);
            
            if (character == null) {
                continue;
            }
            
            // Add sub note skill id to drop list
            int subNoteSkill = character.getData().getElementType().getSubNoteSkillItemId();
            if (subNoteSkill > 0 && !this.subNoteDropList.contains(subNoteSkill)) {
                this.subNoteDropList.add(subNoteSkill);
            }
            
            // Add to character list
            charList.add(id);
        }

        for (int i = 0; i < 6; i++) {
            int id = formation.getDiscIdAt(i);
            var disc = getPlayer().getCharacters().getDiscById(id);
            
            if (disc == null) {
                continue;
            }
            
            // Add star tower sub note skills from disc
            if (i >= 3) {
                var subNoteData = disc.getSubNoteSkillDef();
                if (subNoteData != null) {
                    this.getItems().add(subNoteData.getItems());
                }
            }
            
            // Add to disc list
            discList.add(id);
        }
        
        // Merge char/disc ids into an array for optimization
        this.charIds = charList.toIntArray();
        this.discIds = discList.toIntArray();
        
        // Finish setting up droppable sub note skills
        for (int id : GameConstants.TOWER_COMMON_SUB_NOTE_SKILLS) {
            this.subNoteDropList.add(id);
        }
        
        // Add starting items
        this.getModifiers().addStartingItems();
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
    
    public int getDifficulty() {
        return this.getData().getDifficulty();
    }
    
    public GameCharacter getCharByIndex(int index) {
        if (index < 0 || index >= this.getCharIds().length) {
            return null;
        }
        
        int id = this.getCharIds()[index];
        return this.getPlayer().getCharacters().getCharacterById(id);
    }
    
    public GameDisc getDiscByIndex(int index) {
        if (index < 0 || index >= this.getDiscIds().length) {
            return null;
        }
        
        int id = this.getDiscIds()[index];
        return this.getPlayer().getCharacters().getDiscById(id);
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
    
    public int getResCount(int id) {
        return this.getRes().get(id);
    }
    
    public PlayerChangeInfo addItem(int id, int count) {
        return this.addItem(id, count, null);
    }
    
    public PlayerChangeInfo addItem(int id, int count, PlayerChangeInfo change) {
        // Create changes if null
        if (change == null) {
            change = new PlayerChangeInfo();
        }
        
        // Sanity check
        if (count == 0) {
            return change;
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
                int nextLevel = Math.min(curLevel + count, potentialData.getMaxLevel(this));
                
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
                // Sanity check to make sure we dont remove more than what we have
                if (count < 0) {
                    count = Math.max(count, -this.getItems().get(id));
                }
                
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
                // Sanity check to make sure we dont remove more than what we have
                if (count < 0) {
                    count = Math.max(count, -this.getRes().get(id));
                }
                
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
     * Creates a potential selectors for the client if there are any potential selectors avaliable.
     * If there are none, then create the door case so the player can exit
     */
    public List<StarTowerBaseCase> handlePendingPotentialSelectors() {
        // Create potential selectors if any are avaliable
        if (this.pendingPotentialCases > 0) {
            this.pendingPotentialCases--;
            
            return List.of(this.createPotentialSelector());
        }
        
        // Return empty list if room already has an exit
        if (this.getRoom().hasDoor()) {
            return List.of();
        }
        
        // Init List of next cases
        List<StarTowerBaseCase> cases = new ArrayList<>();
        
        // Add door case here
        cases.add(this.createExit());
        
        // Create shop npc if this is the last room
        if (this.isOnFinalFloor()) {
            // Create hawker case (shop)
            cases.add(new StarTowerHawkerCase());
            // Create strengthen machine
            if (this.getModifiers().isEnableEndStrengthen()) {
                cases.add(new StarTowerStrengthenMachineCase());
            }
        } else if (this.getRoom() instanceof StarTowerBattleRoom) {
            // Create recovery npc
            cases.add(new StarTowerNpcRecoveryHPCase());
        }
        
        // Complete
        return cases;
    }
    
    /**
     * Creates a potential selector for a random character
     */
    public StarTowerPotentialCase createPotentialSelector() {
        return this.createPotentialSelector(0);
    }
    
    public StarTowerPotentialCase createPotentialSelector(int charId) {
        return this.createPotentialSelector(charId, false);
    }
    
    /**
     * Creates a potential selector for the specified character
     */
    public StarTowerPotentialCase createPotentialSelector(int charId, boolean rareOnly) {
        // Check character id
        if (charId <= 0) {
            charId = this.getRandomCharId();
        }
        
        // Get character potentials
        var data = GameData.getCharPotentialDataTable().get(charId);
        if (data == null) {
            return null;
        }
        
        // Random potentials list
        var list = new IntArrayList();
        
        // Add potentials based on character role
        boolean isMainCharacter = this.getCharIds()[0] == charId;
        
        if (isMainCharacter) {
            list.addElements(0, data.getMasterSpecificPotentialIds());
            
            if (!rareOnly) {
                list.addElements(0, data.getMasterNormalPotentialIds());
            }
        } else {
            list.addElements(0, data.getAssistSpecificPotentialIds());
            
            if (!rareOnly) {
                list.addElements(0, data.getAssistNormalPotentialIds());
            }
        }
        
        if (!rareOnly) {
            list.addElements(0, data.getCommonPotentialIds()); 
        }
        
        // Remove potentials we already have maxed out
        var potentials = new IntArrayList();
        
        for (int id : list) {
            // Get potential data
            var potential = GameData.getPotentialDataTable().get(id);
            if (potential == null) continue;
            
            // Filter out max level ones
            int curLevel = this.getPotentials().get(id);
            int maxLevel = potential.getMaxLevel(this);
            
            if (curLevel >= maxLevel) {
                continue;
            }
            
            // Add
            potentials.add(id);
        }
        
        // Get up to 3 random potentials
        List<StarTowerPotentialInfo> selector = new ArrayList<>();
        
        for (int i = 0; i < 3; i++) {
            // Sanity check
            if (potentials.isEmpty()) {
                break;
            }
            
            // Get random potential id
            int id = Utils.randomElement(potentials, true);
            int level = 1;
            
            // Check for bonuses
            if (this.getItemCount(id) > 0) {
                // New potential
                if (Utils.randomChance(this.getModifiers().getBonusPotentialChance())) {
                    level += this.getModifiers().getBonusPotentialLevel();
                }
            } else {
                // Existing potential
                if (Utils.randomChance(this.getModifiers().getBonusStrengthenChance())) {
                    level += 1;
                }
            }
            
            // Create potential
            var potential = new StarTowerPotentialInfo(id, level);
            
            // Add to selector
            selector.add(potential);
        }
        
        // Sanity check
        if (selector.isEmpty()) {
            return null;
        }
        
        // Creator potential selector case
        if (rareOnly) {
            return new StarTowerSelectSpecialPotentialCase(this, charId, selector);
        } else {
            return new StarTowerPotentialCase(this, charId, selector);
        }
    }
    
    public StarTowerPotentialCase createStrengthenSelector() {
        // Random potentials list
        var potentials = new IntArrayList();
        
        // Get upgradable potentials
        for (var item : this.getPotentials()) {
            // Get potential data
            var potential = GameData.getPotentialDataTable().get(item.getIntKey());
            if (potential == null) continue;
            
            // Check max level
            int level = item.getIntValue();
            if (level >= potential.getMaxLevel(this)) {
                continue;
            }
            
            // Add
            potentials.add(potential.getId());
        }
        
        // Get up to 3 random potentials
        List<StarTowerPotentialInfo> selector = new ArrayList<>();
        
        for (int i = 0; i < 3; i++) {
            // Sanity check
            if (potentials.isEmpty()) {
                break;
            }
            
            // Get random potential id
            int id = Utils.randomElement(potentials, true);
            int level = 1;
            
            // Check bonus potential chance
            if (Utils.randomChance(this.getModifiers().getBonusStrengthenChance())) {
                level += 1;
            }
            
            // Create potential
            var potential = new StarTowerPotentialInfo(id, level);
            
            // Add to selector
            selector.add(potential);
        }
        
        // Sanity check
        if (selector.isEmpty()) {
            return null;
        }
        
        // Creator potential selector case
        return new StarTowerPotentialCase(this, true, selector);
    }
    
    public void setPendingSubNotes(int amount) {
        this.pendingSubNotes = amount;
    }
    
    public int getRandomSubNoteId() {
        return Utils.randomElement(this.getSubNoteDropList());
    }
    
    private PlayerChangeInfo addRandomSubNoteSkills(PlayerChangeInfo change) {
        int id = this.getRandomSubNoteId();
        int count = Utils.randomRange(1, 3);
        
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
        
        // Add any sub note skills
        var data = rsp.getMutableData();
        
        if (this.getNewInfos().size() > 0) {
            // Add sub note skills to the proto
            for (var entry : this.getNewInfos()) {
                var info = SubNoteSkillInfo.newInstance()
                        .setTid(entry.getIntKey())
                        .setQty(entry.getIntValue());
                
                data.getMutableInfos().add(info);
            }
            
            // Refresh secondary skills
            var newSecondarySkills = SecondarySkillDef.calculateSecondarySkills(this.getDiscIds(), this.getItems());
            
            // Add any new secondary skills to the data proto
            for (int id : newSecondarySkills) {
                if (!this.getSecondarySkills().contains(id)) {
                    var info = ActiveSecondaryChange.newInstance()
                            .setSecondaryId(id)
                            .setActive(true);
                    
                    data.addSecondaries(info);
                }
            }
            
            // Inform the client that these skills are no longer active
            for (int id : this.getSecondarySkills()) {
                if (!newSecondarySkills.contains(id)) {
                    var info = ActiveSecondaryChange.newInstance()
                            .setSecondaryId(id)
                            .setActive(false);
                    
                    data.addSecondaries(info);
                }
            }
            
            // Set new secondary skills
            this.secondarySkills = newSecondarySkills;
            
            // Clear new infos
            this.getNewInfos().clear();
        }
        
        // Always add this proto
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
        
        var meta = proto.getMutableMeta()
            .setId(this.getId())
            .setCharHp(this.getCharHp())
            .setTeamLevel(this.getTeamLevel())
            .setNPCInteractions(0)
            .setTotalTime(this.getBattleTime())
            .setBuildId(this.getBuildId());
        
        // Set characters + discs
        for (int i = 0; i < 3; i++) {
            var character = this.getCharByIndex(i);
            
            if (character != null) {
                meta.addChars(character.toStarTowerProto());
            } else {
                meta.addChars(StarTowerChar.newInstance());
            }
        }
        for (int i = 0; i < 6; i++) {
            var disc = this.getDiscByIndex(i);
            
            if (disc != null) {
                meta.addDiscs(disc.toStarTowerProto());
            } else {
                meta.addDiscs(StarTowerDisc.newInstance());
            }
        }
        
        // Add secondary skills
        for (int id : this.getSecondarySkills()) {
            meta.addActiveSecondaryIds(id);
        }
        
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
