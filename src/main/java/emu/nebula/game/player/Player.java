package emu.nebula.game.player;

import java.util.HashSet;
import java.util.Set;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Indexed;

import emu.nebula.GameConstants;
import emu.nebula.Nebula;
import emu.nebula.data.GameData;
import emu.nebula.database.GameDatabaseObject;
import emu.nebula.game.account.Account;
import emu.nebula.game.character.CharacterStorage;
import emu.nebula.game.formation.FormationManager;
import emu.nebula.game.gacha.GachaManager;
import emu.nebula.game.instance.InstanceManager;
import emu.nebula.game.inventory.Inventory;
import emu.nebula.game.mail.Mailbox;
import emu.nebula.game.story.StoryManager;
import emu.nebula.game.tower.StarTowerManager;
import emu.nebula.net.GameSession;
import emu.nebula.proto.PlayerData.DictionaryEntry;
import emu.nebula.proto.PlayerData.DictionaryTab;
import emu.nebula.proto.PlayerData.PlayerInfo;
import emu.nebula.proto.Public.Energy;
import emu.nebula.proto.Public.NewbieInfo;
import emu.nebula.proto.Public.QuestType;
import emu.nebula.proto.Public.Story;
import emu.nebula.proto.Public.WorldClass;
import emu.nebula.proto.Public.Title;

import lombok.Getter;
import us.hebi.quickbuf.RepeatedInt;

@Getter
@Entity(value = "players", useDiscriminator = false)
public class Player implements GameDatabaseObject {
    @Id private int uid;
    @Indexed private String accountUid;
    
    private transient Account account;
    private transient Set<GameSession> sessions;
    
    // Details
    private String name;
    private String signature;
    private boolean gender;
    private int headIcon;
    private int skinId;
    private int titlePrefix;
    private int titleSuffix;
    private int level;
    private int exp;
    private int[] boards;
    
    private int energy;
    private long energyLastUpdate;
   
    private long createTime;
    
    // Managers
    private final transient CharacterStorage characters;
    private transient GachaManager gachaManager;
    
    // Referenced data
    private transient Inventory inventory;
    private transient FormationManager formations;
    private transient Mailbox mailbox;
    private transient StarTowerManager starTowerManager;
    private transient InstanceManager instanceManager;
    private transient StoryManager storyManager;
    
    @Deprecated // Morphia only
    public Player() {
        this.sessions = new HashSet<>();
        this.characters = new CharacterStorage(this);
        this.gachaManager = new GachaManager(this);
    }
    
    public Player(Account account, String name, boolean gender) {
        this();
        
        // Set uid first
        if (account.getReservedPlayerUid() > 0) {
            this.uid = account.getReservedPlayerUid();
        } else {
            this.uid = Nebula.getGameDatabase().getNextObjectId(Player.class);
        }
        
        // Set basic info
        this.accountUid = account.getUid();
        this.createTime = Nebula.getCurrentTime();
        this.name = name;
        this.signature = "";
        this.gender = gender;
        this.headIcon = 101;
        this.skinId = 10301;
        this.titlePrefix = 1;
        this.titleSuffix = 2;
        this.level = 1;
        this.energy = 240;
        this.energyLastUpdate = this.createTime;
        this.boards = new int[] {410301};
        
        // Setup inventory
        this.inventory = new Inventory(this);
        
        // Add starter characters
        this.getCharacters().addCharacter(103);
        this.getCharacters().addCharacter(112);
        this.getCharacters().addCharacter(113);
        
        // Add starter discs
        this.getCharacters().addDisc(211001);
        this.getCharacters().addDisc(211005);
        this.getCharacters().addDisc(211007);
        this.getCharacters().addDisc(211008);
    }
    
    public Account getAccount() {
        if (this.account == null) {
            this.account = Nebula.getAccountDatabase().getObjectByField(Account.class, "_id", this.getAccountUid());
        }
        
        return this.account;
    }
    
    public void addSession(GameSession session) {
        synchronized (this.sessions) {
            this.sessions.add(session);
        }
    }
    
    public void removeSession(GameSession session) {
        synchronized (this.sessions) {
            this.sessions.remove(session);
        }
    }
    
    public boolean hasSessions() {
        synchronized (this.sessions) {
            return !this.sessions.isEmpty();
        }
    }
    
    public boolean getGender() {
        return this.gender;
    }

    public boolean editName(String newName) {
        // Sanity check
        if (newName == null || newName.isEmpty() || newName.equals(this.getName())) {
            return false;
        }
        
        // Limit name length
        if (newName.length() > 20) {
            newName = newName.substring(0, 19);
        }
        
        // Set name
        this.name = newName;
        
        // Update in database
        Nebula.getGameDatabase().update(this, this.getUid(), "name", this.getName());
        
        // Success
        return true;
    }
    
    public void editGender() {
        // Set name
        this.gender = !this.gender;
        
        // Update in database
        Nebula.getGameDatabase().update(this, this.getUid(), "gender", this.getGender());
    }

    public boolean editTitle(int prefix, int suffix) {
        // Check to make sure we own these titles
        if (!getInventory().getTitles().contains(prefix) || !getInventory().getTitles().contains(suffix)) {
            return false;
        }
        
        // Skip if we are not changing titles
        if (this.titlePrefix == prefix && this.titleSuffix == suffix) {
            return true;
        }
        
        // Set
        this.titlePrefix = prefix;
        this.titleSuffix = suffix;
        
        // Update in database
        Nebula.getGameDatabase().update(this, this.getUid(), "titlePrefix", this.getTitlePrefix(), "titleSuffix", this.getTitleSuffix());
        
        return true;
    }
    
    public boolean editHeadIcon(int id) {
        // Skip if we are not changing head icon
        if (this.headIcon == id) {
            return true;
        }
        
        // Make sure we own the head icon
        if (!getInventory().hasHeadIcon(id)) {
            return false;
        }
        
        // Set
        this.headIcon = id;
        
        // Update in database
        Nebula.getGameDatabase().update(this, this.getUid(), "headIcon", this.getHeadIcon());
        
        // Success
        return true;
    }
    
    public boolean editSignature(String signature) {
        // Sanity check
        if (signature == null) {
            return false;
        }
        
        // Limit signature to 30 max chars
        if (signature.length() > 30) {
            signature = signature.substring(0, 29);
        }
        
        // Set signature
        this.signature = signature;
        
        // Update in database
        Nebula.getGameDatabase().update(this, this.getUid(), "signature", this.getSignature());
        
        // Success
        return true;
    }

    public boolean setBoard(RepeatedInt ids) {
        // Length check
        if (ids.length() <= 0 || ids.length() > GameConstants.MAX_SHOWCASE_IDS) {
            return false;
        }
        
        // Get max length
        this.boards = new int[ids.length()];
        
        // Copy ids to our boards array
        for (int i = 0; i < ids.length(); i++) {
            int id = ids.get(i);
            this.boards[i] = id;
        }
        
        // Save to database
        Nebula.getGameDatabase().update(this, this.getUid(), "boards", this.getBoards());
        
        // Success
        return true;
    }
    
    public void setNewbieInfo(int groupId, int stepId) {
        // TODO 
    }
    
    public int getMaxExp() {
        var data = GameData.getWorldClassDataTable().get(this.level + 1);
        return data != null ? data.getExp() : 0;
    }
    
    public PlayerChangeInfo addExp(int amount, PlayerChangeInfo changes) {
        // Check if changes is null
        if (changes == null) {
            changes = new PlayerChangeInfo();
        }
        
        // Sanity
        if (amount <= 0) {
            return changes;
        }
        
        // Setup
        int oldLevel = this.getLevel();
        int oldExp = this.getExp();
        int expRequired = this.getMaxExp();

        // Add exp
        this.exp += amount;

        // Check for level ups
        while (this.exp >= expRequired && expRequired > 0) {
            this.level += 1;
            this.exp -= expRequired;
            
            expRequired = this.getMaxExp();
        }
        
        // Save to database
        Nebula.getGameDatabase().update(
                this, 
                this.getUid(), 
                "level", 
                this.getLevel(), 
                "exp", 
                this.getExp()
        );
        
        // Calculate changes
        var proto = WorldClass.newInstance()
                .setAddClass(this.getLevel() - oldLevel)
                .setExpChange(this.getExp() - oldExp);
        
        changes.add(proto);
        
        return changes;
    }
    
    // Energy
    
    public int getEnergy() {
        // Cache time
        long time = Nebula.getCurrentTime();
        
        // Calculate time diff
        double diff = time - this.energyLastUpdate;
        long bonusEnergy = (int) Math.floor(diff / GameConstants.ENERGY_REGEN_TIME);
        
        if (this.energy < GameConstants.MAX_ENERGY) {
            this.energy = Math.min(this.energy + (int) bonusEnergy, GameConstants.MAX_ENERGY);
            this.energyLastUpdate = (bonusEnergy * GameConstants.ENERGY_REGEN_TIME) + this.energyLastUpdate;
        } else {
            this.energyLastUpdate = time;
        }
        
        return this.energy;
    }
    
    public PlayerChangeInfo addEnergy(int amount, PlayerChangeInfo change) {
        // Sanity check
        if (amount <= 0) {
            return change == null ? new PlayerChangeInfo() : change;
        }
        
        // Complete
        return modifyEnergy(amount, change);
    }
    
    public PlayerChangeInfo consumeEnergy(int amount, PlayerChangeInfo change) {
        // Sanity check
        if (amount <= 0) {
            return change == null ? new PlayerChangeInfo() : change;
        }
        
        // Complete
        return modifyEnergy(-amount, change);
    }
    
    private PlayerChangeInfo modifyEnergy(int amount, PlayerChangeInfo change) {
        // Check if changes is null
        if (change == null) {
            change = new PlayerChangeInfo();
        }
        
        // Update energy
        this.getEnergy();
        
        // Remove energy
        this.energy = Math.max(this.energy + amount, 0);
        
        // Save to database
        Nebula.getGameDatabase().update(
                this, 
                this.getUid(), 
                "energy", 
                this.getEnergy(), 
                "energyLastUpdate", 
                this.getEnergyLastUpdate()
        );
        
        // Add to change
        change.add(this.getEnergyProto());
        
        // Complete
        return change;
    }
    
    //
    
    public void sendMessage(String string) {
        // Empty
    }
    
    // Login
    
    private <T extends PlayerManager> T loadManagerFromDatabase(Class<T> cls) {
        var manager = Nebula.getGameDatabase().getObjectByField(cls, "_id", this.getUid());
        
        if (manager != null) {
            manager.setPlayer(this);
        } else {
            try {
                manager = cls.getDeclaredConstructor(Player.class).newInstance(this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        return manager;
    }
    
    public void onLoad() {
        // Load from database
        this.getCharacters().loadFromDatabase();
        
        // Load inventory first
        if (this.inventory == null) {
            this.inventory = this.loadManagerFromDatabase(Inventory.class);
        }
        this.getInventory().loadFromDatabase();
        
        // Load referenced classes
        this.formations = this.loadManagerFromDatabase(FormationManager.class);
        this.mailbox = this.loadManagerFromDatabase(Mailbox.class);
        this.starTowerManager = this.loadManagerFromDatabase(StarTowerManager.class);
        this.instanceManager = this.loadManagerFromDatabase(InstanceManager.class);
        this.storyManager = this.loadManagerFromDatabase(StoryManager.class);
    }
    
    // Proto

    public PlayerInfo toProto() {
        PlayerInfo proto = PlayerInfo.newInstance()
                .setServerTs(Nebula.getCurrentTime())
                .setAchievements(new byte[64]);
        
        var acc = proto.getMutableAcc()
            .setNickName(this.getName())
            .setSignature(this.getSignature())
            .setGender(this.getGender())
            .setId(this.getUid())
            .setHeadIcon(this.getHeadIcon())
            .setSkinId(this.getSkinId())
            .setTitlePrefix(this.getTitlePrefix())
            .setTitleSuffix(this.getTitleSuffix())
            .setCreateTime(this.getCreateTime());
        
        proto.getMutableWorldClass()
            .setCur(this.getLevel())
            .setLastExp(this.getExp());
        
        proto.getMutableEnergy().setEnergy(this.getEnergyProto());
        
        // Add characters/discs/res/items
        for (var character : getCharacters().getCharacterCollection()) {
            proto.addChars(character.toProto());
        }
        
        for (var disc : getCharacters().getDiscCollection()) {
            proto.addDiscs(disc.toProto());
        }
        
        for (var item : getInventory().getItems().values()) {
            proto.addItems(item.toProto());
        }
        
        for (var res : getInventory().getResources().values()) {
            proto.addRes(res.toProto());
        }
        
        // Formations
        for (var f : this.getFormations().getFormations().values()) {
            proto.getMutableFormation().addInfo(f.toProto());
        }
        
        // Set state
        var state = proto.getMutableState()
            .setStorySet(true);
        
        state.getMutableMail();
        state.getMutableBattlePass();
        state.getMutableWorldClassReward();
        state.getMutableFriendEnergy();
        state.getMutableMallPackage();
        state.getMutableAchievement();
        state.getMutableTravelerDuelQuest()
            .setType(QuestType.TravelerDuel);
        state.getMutableTravelerDuelChallengeQuest()
            .setType(QuestType.TravelerDuelChallenge);
        state.getMutableStarTower();
        state.getMutableStarTowerBook();
        state.getMutableScoreBoss();
        state.getMutableCharAffinityRewards();
        
        // Force complete tutorials
        for (var guide : GameData.getGuideGroupDataTable()) {
            var info = NewbieInfo.newInstance()
                    .setGroupId(guide.getId())
                    .setStepId(-1);
            
            acc.addNewbies(info);
        }
        
        acc.addNewbies(NewbieInfo.newInstance().setGroupId(GameConstants.INTRO_GUIDE_ID).setStepId(-1));
        
        // Story
        var story = proto.getMutableStory();
        
        for (int storyId : this.getStoryManager().getCompletedStories()) {
            var storyProto = Story.newInstance()
                    .setIdx(storyId);
            
            story.addStories(storyProto);
        }
        
        // Add titles
        for (int titleId : this.getInventory().getTitles()) {
            var titleProto = Title.newInstance()
                    .setTitleId(titleId);
            
            proto.addTitles(titleProto);
        }
        
        // Add board ids
        for (int boardId : this.getBoards()) {
            proto.addBoard(boardId);
        }
        
        // Add dictionary tabs
        for (var dictionaryData : GameData.getDictionaryTabDataTable()) {
            var dictionaryProto = DictionaryTab.newInstance()
                    .setTabId(dictionaryData.getId());
            
            for (var entry : dictionaryData.getEntries()) {
                var entryProto = DictionaryEntry.newInstance()
                        .setIndex(entry.getIndex())
                        .setStatus(2); // 2 = complete
                
                dictionaryProto.addEntries(entryProto);
            }
            
            proto.addDictionaries(dictionaryProto);
        }
        
        // Add instances
        this.getInstanceManager().toProto(proto);
        
        // Handbook
        proto.addHandbook(this.getCharacters().getCharacterHandbook());
        proto.addHandbook(this.getCharacters().getDiscHandbook());
        
        // Extra
        proto.getMutableVampireSurvivorRecord()
            .getMutableSeason();
        
        proto.getMutableQuests();
        proto.getMutableAgent();
        proto.getMutablePhone();
        
        return proto;
    }
    
    public Energy getEnergyProto() {
        long nextDuration = Math.max(GameConstants.ENERGY_REGEN_TIME - (Nebula.getCurrentTime() - getEnergyLastUpdate()), 1);
        
        var proto = Energy.newInstance()
                .setUpdateTime(this.getEnergyLastUpdate())
                .setNextDuration(nextDuration)
                .setPrimary(this.getEnergy())
                .setIsPrimary(true);
        
        return proto;
    }
}
