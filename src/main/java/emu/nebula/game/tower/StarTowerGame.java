package emu.nebula.game.tower;

import java.util.ArrayList;
import java.util.List;

import dev.morphia.annotations.Entity;

import emu.nebula.GameConstants;
import emu.nebula.Nebula;
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

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
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
    private int nextLevelExp;
    private int charHp;
    private int battleTime;
    private int battleCount;
    private List<StarTowerChar> chars;
    private List<StarTowerDisc> discs;
    private IntList charIds;
    
    // Case
    private int lastCaseId = 0;
    private List<StarTowerCase> cases;
    private Int2IntMap cachedCases;
    private int pendingPotentialCases = 0;
    
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
        this.teamExp = 0;
        this.nextLevelExp = GameData.getStarTowerTeamExpDataTable().get(2).getNeedExp();
        this.stageNum = 1;
        this.stageFloor = 1;
        this.floor = 1;
        this.charHp = -1;
        this.chars = new ArrayList<>();
        this.discs = new ArrayList<>();
        this.charIds = new IntArrayList();

        this.cases = new ArrayList<>();
        this.cachedCases = new Int2IntOpenHashMap();
        
        this.items = new ItemParamMap();
        this.res = new ItemParamMap();
        this.potentials = new ItemParamMap();
        this.newInfos = new ItemParamMap();
        
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
        
        // Add cases
        this.addCase(new StarTowerCase(CaseType.Battle));
        this.addCase(new StarTowerCase(CaseType.SyncHP));
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

    public int levelUp(int picks) {
        if (this.teamExp >= this.nextLevelExp) {
            // Level up
            this.teamLevel++;

            // Add 1 to pending potential picks
            picks++;

            // Subtract target exp
            this.teamExp = this.teamExp - this.nextLevelExp;

            // Update next level exp 
            this.nextLevelExp = GameData.getStarTowerTeamExpDataTable().get(this.teamLevel + 1).getNeedExp();
            
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
    
    // Cases
    
    public StarTowerCase getCase(CaseType type) {
        // Check if we have any cached case for this case type
        if (!this.getCachedCases().containsKey(type.getValue())) {
            return null;
        }
        
        // Get index of cached case
        int index = this.getCachedCases().get(type.getValue());
        
        // Sanity check just in case
        if (index < 0 || index >= this.getCases().size()) {
            return null;
        }
        
        return this.getCases().get(index);
    }
    
    public void cacheCaseIndex(StarTowerCase towerCase) {
        this.getCachedCases().put(towerCase.getType().getValue(), this.getCases().size() - 1);
    }
    
    public StarTowerCase addCase(StarTowerCase towerCase) {
        return this.addCase(null, towerCase);
    }
    
    public StarTowerCase addCase(RepeatedMessage<StarTowerRoomCase> cases, StarTowerCase towerCase) {
        // Add to cases list
        this.getCases().add(towerCase);
        
        // Increment id
        towerCase.setId(++this.lastCaseId);
        
        // Set proto
        if (cases != null) {
            cases.add(towerCase.toProto());
        }
        
        // Cache case index
        this.cacheCaseIndex(towerCase);
        
        // Complete
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
        int id = Utils.randomElement(this.getSubNoteDropList());
        
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
        } else if (req.hasHawkerReq()) {
            rsp = this.onHawkerReq(req, rsp);
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
        // Parse battle end
        var proto = req.getBattleEndReq();
        
        // Init change
        var change = new PlayerChangeInfo();
        
        // Handle victory/defeat
        if (proto.hasVictory()) {
            // Handle leveling up

            // Get relevant floor exp data
            // fishiatee: THERE'S NO LINQ IN JAVAAAAAAAAAAAAA
            var floorExpData = GameData.getStarTowerFloorExpDataTable().stream()
                                .filter(f -> f.getStarTowerId() == this.getId())
                                .findFirst()
                                .orElseThrow();
            int expReward = 0;

            // Determine appropriate exp reward
            switch (this.getRoomType()) {
                // Regular battle room
                case 0:
                    expReward = floorExpData.getNormalExp();
                    break;
                // Elite battle room
                case 1:
                    expReward = floorExpData.getEliteExp();
                    break;
                // Non-final boss room
                case 2:
                    expReward = floorExpData.getBossExp();
                    break;
                // Final room
                case 3:
                    expReward = floorExpData.getFinalBossExp();
                    break;
            }

            // Level up
            this.teamExp += expReward;
            this.pendingPotentialCases += this.levelUp();
            
            // Add clear time
            this.battleTime += proto.getVictory().getTime();
            
            // Handle victory
            rsp.getMutableBattleEndResp()
                .getMutableVictory()
                .setLv(this.getTeamLevel())
                .setBattleTime(this.getBattleTime());
            
            // Add money
            int money = this.getStageData(this.getStageNum(), this.getStageFloor()).getInteriorCurrencyQuantity();
            
            this.addItem(GameConstants.STAR_TOWER_GOLD_ITEM_ID, money, change);
            
            // Handle pending potential selectors
            if (this.pendingPotentialCases > 0) {
                // Create potential selector
                var potentialCase = this.createPotentialSelector(this.getRandomCharId());
                this.addCase(rsp.getMutableCases(), potentialCase);
                
                this.pendingPotentialCases--;
            }
            else {
                // Add door case here
                var doorCase = this.addCase(new StarTowerCase(CaseType.OpenDoor));
                doorCase.setFloorId(this.getStageFloor() + 1);
        
                var nextStage = this.getNextStageData();
                if (nextStage != null) {
                    doorCase.setRoomType(nextStage.getRoomType());
                }

                this.addCase(rsp.getMutableCases(), doorCase);
            }
            
            // Add sub note skills
            var battleCase = this.getCase(CaseType.Battle);
            if (battleCase != null) {
                int subNoteSkills = battleCase.getSubNoteSkillNum();
                this.addRandomSubNoteSkills(subNoteSkills, change);
            }

        } else {
            // Handle defeat
            // TODO
        }
        
        // Increment battle count
        this.battleCount++;
        
        // Set change
        rsp.setChange(change.toProto());
        
        return rsp;
    }

    public StarTowerInteractResp onSelect(StarTowerInteractReq req, StarTowerInteractResp rsp) {
        var index = req.getMutableSelectReq().getIndex();
        
        var selectorCase = this.getCase(CaseType.SelectSpecialPotential);
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
        
        // Handle pending potential selectors
        if (this.pendingPotentialCases > 0) {
            // Create potential selector
            var potentialCase = this.createPotentialSelector(this.getRandomCharId());
            this.addCase(rsp.getMutableCases(), potentialCase);

            this.pendingPotentialCases--;
        }
        else {
            // Add door case
            var doorCase = this.addCase(new StarTowerCase(CaseType.OpenDoor));
            doorCase.setFloorId(this.getStageFloor() + 1);
        
            var nextStage = this.getNextStageData();
            if (nextStage != null) {
                doorCase.setRoomType(nextStage.getRoomType());
            }

            this.addCase(rsp.getMutableCases(), doorCase);
        }
        
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
        this.lastCaseId = 0;
        this.cases.clear();
        this.cachedCases.clear();
        
        // Add cases
        var syncHpCase = new StarTowerCase(CaseType.SyncHP);
        
        // Room proto
        var room = rsp.getMutableEnterResp().getMutableRoom();
        room.setData(this.toRoomDataProto());
        
        // Handle room type TODO
        if (this.roomType <= StarTowerRoomType.FinalBossRoom.getValue()) {
            var battleCase = new StarTowerCase(CaseType.Battle);
            battleCase.setSubNoteSkillNum(Utils.randomRange(1, 3));

            this.addCase(room.getMutableCases(), battleCase);
        } else if (this.roomType == StarTowerRoomType.EventRoom.getValue()) {
            
        } else if (this.roomType == StarTowerRoomType.ShopRoom.getValue()) {
            var hawkerCase = new StarTowerCase(CaseType.Hawker);
            
            // TODO
            hawkerCase.addGoods(new StarTowerShopGoods(1, 1, 200));
            hawkerCase.addGoods(new StarTowerShopGoods(1, 1, 200));
            hawkerCase.addGoods(new StarTowerShopGoods(1, 1, 200));
            
            this.addCase(room.getMutableCases(), hawkerCase);
        }
        
        // Add cases
        this.addCase(room.getMutableCases(), syncHpCase);

        // Add door to next floor if current floor is choice/shop domains
        if (this.roomType == 6 | this.roomType == 7) {
            var doorCase = new StarTowerCase(CaseType.OpenDoor);
            doorCase.setFloorId(this.getFloor() + 1);

            // Set room type of next room
            var nextStage = this.getNextStageData();
            if (nextStage != null) {
                doorCase.setRoomType(nextStage.getRoomType());
            }

            // Add case
            this.addCase(room.getMutableCases(), doorCase);
        }
        
        // Done
        return rsp;
    }

    public StarTowerInteractResp onRecoveryHP(StarTowerInteractReq req, StarTowerInteractResp rsp) {
        // Add case
        this.addCase(rsp.getMutableCases(), new StarTowerCase(CaseType.RecoveryHP));
        
        return rsp;
    }
    
    private StarTowerInteractResp onHawkerReq(StarTowerInteractReq req, StarTowerInteractResp rsp) {
        // Set this proto
        rsp.getMutableNilResp();
        
        // Get hawker case
        var shop = this.getCase(CaseType.Hawker);
        if (shop == null || shop.getGoodsList() == null) {
            return rsp;
        }
        
        // Get goods
        var goods = shop.getGoodsList().get(req.getHawkerReq().getSid());
        
        // Make sure we have enough currency
        if (this.getRes().get(GameConstants.STAR_TOWER_GOLD_ITEM_ID) < goods.getPrice() || goods.isSold()) {
            return rsp;
        }
        
        // Mark goods as sold
        goods.markAsSold();
        
        // Add case
        int charId = this.getRandomCharId();
        var potentialCase = this.createPotentialSelector(charId);
        this.addCase(rsp.getMutableCases(), potentialCase);
        
        // Remove items
        var change = this.addItem(GameConstants.STAR_TOWER_GOLD_ITEM_ID, -goods.getPrice(), null);
        
        // Set change info
        rsp.setChange(change.toProto());
        
        // Success
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
        
        // Log victory
        this.getManager().getPlayer().getProgress().addStarTowerLog(this.getId());
        
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
        
        for (var entry : this.getRes()) {
            var res = TowerResInfo.newInstance()
                    .setTid(entry.getIntKey())
                    .setQty(entry.getIntValue());
            
            bag.addRes(res);
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
