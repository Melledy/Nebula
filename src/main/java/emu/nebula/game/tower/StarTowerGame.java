package emu.nebula.game.tower;

import java.util.ArrayList;
import java.util.List;

import dev.morphia.annotations.Entity;

import emu.nebula.GameConstants;
import emu.nebula.data.GameData;
import emu.nebula.data.resources.SecondarySkillDef;
import emu.nebula.data.resources.StarTowerDef;
import emu.nebula.data.resources.StarTowerStageDef;
import emu.nebula.game.achievement.AchievementCondition;
import emu.nebula.game.achievement.AchievementManager;
import emu.nebula.game.character.ElementType;
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
    private int pendingRarePotentialCases = 0;
    private boolean completed;
    
    // Bag
    private ItemParamMap items;
    private ItemParamMap res;
    private ItemParamMap potentials;
    private IntSet secondarySkills;
    
    // Sub note skill drop list
    private IntList subNoteDropList;
    
    // Modifiers
    private StarTowerModifiers modifiers;
    
    // Cached info
    private transient StarTowerBuild build;
    private transient ItemParamMap newInfos;
    private transient ItemParamMap rarePotentialCount;
    
    @Deprecated // Morphia only
    public StarTowerGame() {
        
    }
    
    public StarTowerGame(StarTowerManager manager, StarTowerDef data, Formation formation, StarTowerApplyReq req) {
        // Set manager and cache resource data
        this.manager = manager;
        this.data = data;
        
        // Set tower id
        this.id = req.getId();
        
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
        
        // Temp data to cache for rare potential count
        this.rarePotentialCount = new ItemParamMap();
        
        // Finish setting up droppable sub note skills
        for (int id : GameConstants.TOWER_COMMON_SUB_NOTE_SKILLS) {
            this.subNoteDropList.add(id);
        }
        
        // Enter first room
        this.enterNextRoom();
        this.getRoom().setMapInfo(req);
        
        // Add starting items
        this.getModifiers().addStartingItems();
    }
    
    public Player getPlayer() {
        return this.manager.getPlayer();
    }
    
    public AchievementManager getAchievementManager() {
        return this.getPlayer().getAchievementManager();
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
    
    public int getRarePotentialCount(int charId) {
        return this.getRarePotentialCount().get(charId);
    }
    
    public int getTotalPotentialCount() {
        return this.getItems().values().intStream().reduce(0, Integer::sum);
    }
    
    /**
     * Gets the team element, if the team has 2+ or more elements, then returns null
     */
    public ElementType getTeamElement() {
        ElementType type = null;
        
        for (int id : this.getCharIds()) {
            var character = this.getPlayer().getCharacters().getCharacterById(id);
            if (character == null) {
                return null;
            }
            
            if (type == null) {
                type = character.getData().getElementType();
                continue;
            }
            
            if (type != character.getData().getElementType()) {
                return null;
            }
        }
        
        return type;
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
        var roomType = stage.getRoomType();
        
        if (roomType.getValue() <= RoomType.FinalBossRoom.getValue()) {
            this.room = new StarTowerBattleRoom(this, stage);
        } else if (roomType == RoomType.EventRoom) {
            this.room = new StarTowerEventRoom(this, stage);
        } else if (roomType == RoomType.ShopRoom) {
            this.room = new StarTowerHawkerRoom(this, stage);
        } else {
            this.room = new StarTowerBaseRoom(this, stage);
        }
        
        // Trigger achievement
        this.getAchievementManager().trigger(
            AchievementCondition.TowerEnterRoom,
            1,
            stage.getRoomType().getValue() + 1,
            0
        );
        
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
                
                // Add to rare potential count
                if (potentialData.isRare()) {
                    this.getRarePotentialCount().add(potentialData.getCharId(), 1);
                }
                
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
                
                // Achievment
                if (count > 0) {
                    this.getAchievementManager().trigger(AchievementCondition.TowerItemsGet, count, id, 0);
                }
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
                
                // Achievment
                if (count > 0) {
                    this.getAchievementManager().trigger(AchievementCondition.TowerItemsGet, count, id, 0);
                }
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
        if (amount <= 0) return;
        this.pendingPotentialCases += amount;
    }
    
    public void addRarePotentialSelectors(int amount) {
        if (amount <= 0) return;
        this.pendingRarePotentialCases += amount;
    }
    
    /**
     * Creates a potential selectors for the client if there are any potential selectors avaliable.
     * If there are none, then create the door case so the player can exit
     */
    public List<StarTowerBaseCase> handlePendingPotentialSelectors() {
        // Create rare potential selectors if any characters can recieve a rare potential
        if (this.pendingRarePotentialCases > 0) {
            this.pendingRarePotentialCases--;
            
            // Create a rare selector, if no selector can be created, then create a regular one instead
            var selector = this.createRarePotentialSelector();
            
            if (selector != null) {
                return List.of(selector);
            } else {
                this.pendingPotentialCases += 1;
            }
        }
        
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
        if (this.getRoom().getType() == RoomType.FinalBossRoom) {
            // Create hawker case (shop)
            cases.add(new StarTowerHawkerCase());
            // Create strengthen machine
            if (this.getModifiers().isEnableEndStrengthen()) {
                cases.add(new StarTowerStrengthenMachineCase());
            }
        } else if (this.getRoom() instanceof StarTowerBattleRoom) {
            if (this.getRoom().getType() == RoomType.BattleRoom && Utils.randomChance(this.getModifiers().getBattleNpcEventChance())) {
                // Create npc event
                cases.add(this.getRoom().createNpcEvent());
            } else {
                // Create recovery npc
                cases.add(new StarTowerNpcRecoveryHPCase());
            }
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
    public StarTowerPotentialCase createPotentialSelector(int charId, boolean rare) {
        // Check character id
        if (charId <= 0) {
            charId = this.getRandomCharId();
        }
        
        // Make sure character can't have more than 2 rare potentials
        if (rare && this.getRarePotentialCount(charId) >= 2) {
            return null;
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
            if (rare) {
                list.addElements(0, data.getMasterSpecificPotentialIds());
            } else {
                list.addElements(0, data.getMasterNormalPotentialIds());
            }
        } else {
            if (rare) {
                list.addElements(0, data.getAssistSpecificPotentialIds());
            } else {
                list.addElements(0, data.getAssistNormalPotentialIds());
            }
        }
        
        if (!rare) {
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
        if (rare) {
            return new StarTowerSelectSpecialPotentialCase(this, charId, selector);
        } else {
            return new StarTowerPotentialCase(this, charId, selector);
        }
    }
    
    public int getRandomCharIdForRarePotential() {
        // Create list of avaliable characters
        IntList list = new IntArrayList();
        
        for (int id : this.getCharIds()) {
            int rareCount = this.getRarePotentialCount().get(id);
            if (rareCount < 2) {
                list.add(id);
            }
        }
        
        // Sanity check
        if (list.isEmpty()) {
            return 0;
        }
        
        return Utils.randomElement(list);
    }
    
    /**
     * Creates a random rare potential selector
     */
    public StarTowerPotentialCase createRarePotentialSelector() {
        // Get random character from list
        int charId = this.getRandomCharIdForRarePotential();
        if (charId == 0) return null;
        
        // Create rare selector
        return this.createPotentialSelector(charId, true);
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
    
    public int getRandomSubNoteId() {
        return Utils.randomElement(this.getSubNoteDropList());
    }
    
    private PlayerChangeInfo addRandomSubNoteSkills(PlayerChangeInfo change) {
        // Add sub note with random id
        this.addItem(this.getRandomSubNoteId(), 3, change);
        
        // Complete
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
            this.refreshSecondarySkills(data);
            
            // Clear new infos
            this.getNewInfos().clear();
        }
        
        // Always add this proto
        rsp.getMutableChange();
        
        // Return response proto
        return rsp;
    }
    
    public StarTowerInteractResp settle(StarTowerInteractResp rsp, boolean victory) {
        // Set completed flag
        this.completed = true;
        
        // End game
        this.getManager().settleGame(victory);
        
        // Settle info
        var settle = rsp.getMutableSettle()
                .setTotalTime(this.getBattleTime())
                .setBuild(this.getBuild().toProto());
        
        // Set empty change info
        settle.getMutableChange();
        
        // Calculate rewards
        if (victory) {
            // Init rewards
            var rewards = new ItemParamMap();
            
            // Add journey tickets
            int tickets = this.getModifiers().calculateTickets();
            rewards.add(12, tickets);
            
            // (Custom) Add research materials since tower quests are not implemented yet
            int research = 50 + (Utils.randomRange(this.getDifficulty() - 1, this.getDifficulty() * 2) * 10);
            rewards.add(51, research);
            
            // Add to inventory
            var change = this.getPlayer().getInventory().addItem(51, research);
            
            // Set proto data
            settle.setChange(change.toProto());
            rewards.toItemTemplateStream().forEach(settle::addTowerRewards);
            
            // Save tower tickets
            this.getPlayer().getProgress().addWeeklyTowerTicketLog(tickets);
        }
        
        // Complete
        return rsp;
    }
    
    // Etc
    
    private void refreshSecondarySkills(TowerChangeData data) {
        // Init
        var newSecondarySkills = SecondarySkillDef.calculateSecondarySkills(this.getDiscIds(), this.getItems());
        int newSecondaryCount = 0;
        
        // Add any new secondary skills to the data proto
        for (int id : newSecondarySkills) {
            if (!this.getSecondarySkills().contains(id)) {
                var info = ActiveSecondaryChange.newInstance()
                        .setSecondaryId(id)
                        .setActive(true);
                
                data.addSecondaries(info);
                
                // Counter
                newSecondaryCount++;
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
        
        // Achievement trigger
        if (newSecondaryCount > 0) {
            this.getAchievementManager().trigger(
                AchievementCondition.TowerSpecificSecondarySkillActivateTotal,
                newSecondaryCount
            );
        }
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
