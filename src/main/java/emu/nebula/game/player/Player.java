package emu.nebula.game.player;

import java.util.Stack;

import dev.morphia.annotations.AlsoLoad;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Indexed;

import emu.nebula.GameConstants;
import emu.nebula.Nebula;
import emu.nebula.data.GameData;
import emu.nebula.database.GameDatabaseObject;
import emu.nebula.game.account.Account;
import emu.nebula.game.achievement.AchievementCondition;
import emu.nebula.game.achievement.AchievementManager;
import emu.nebula.game.activity.ActivityManager;
import emu.nebula.game.agent.AgentManager;
import emu.nebula.game.battlepass.BattlePassManager;
import emu.nebula.game.character.CharacterStorage;
import emu.nebula.game.dating.DatingManager;
import emu.nebula.game.formation.FormationManager;
import emu.nebula.game.friends.FriendList;
import emu.nebula.game.gacha.GachaManager;
import emu.nebula.game.infinitytower.InfinityTowerManager;
import emu.nebula.game.instance.InstanceManager;
import emu.nebula.game.inventory.Inventory;
import emu.nebula.game.mail.Mailbox;
import emu.nebula.game.quest.QuestCondition;
import emu.nebula.game.quest.QuestManager;
import emu.nebula.game.scoreboss.ScoreBossManager;
import emu.nebula.game.story.StoryManager;
import emu.nebula.game.tower.StarTowerManager;
import emu.nebula.game.vampire.VampireSurvivorManager;
import emu.nebula.net.GameSession;
import emu.nebula.net.NetMsgId;
import emu.nebula.net.NetMsgPacket;
import emu.nebula.proto.Notify.SigninRewardUpdate;
import emu.nebula.proto.PlayerData.DictionaryEntry;
import emu.nebula.proto.PlayerData.DictionaryTab;
import emu.nebula.proto.PlayerData.PlayerInfo;
import emu.nebula.proto.Public.CharShow;
import emu.nebula.proto.Public.Energy;
import emu.nebula.proto.Public.Friend;
import emu.nebula.proto.Public.HonorInfo;
import emu.nebula.proto.Public.NewbieInfo;
import emu.nebula.proto.Public.QuestType;
import emu.nebula.proto.Public.WorldClass;
import emu.nebula.proto.Public.WorldClassRewardState;
import emu.nebula.util.Utils;
import emu.nebula.proto.Public.Title;

import lombok.Getter;
import us.hebi.quickbuf.ProtoMessage;
import us.hebi.quickbuf.RepeatedInt;

@Getter
@Entity(value = "players", useDiscriminator = false)
public class Player implements GameDatabaseObject {
    @Id private int uid;
    @Indexed private String accountUid;
    
    private transient Account account;
    private transient GameSession session;
    
    @Indexed
    @AlsoLoad("playerRemoteToken")
    private String remoteToken;
    
    // Details
    private String name;
    private String signature;
    private boolean gender;
    private int headIcon;
    private int skinId;
    private int titlePrefix;
    private int titleSuffix;
    private long music;
    private int[] honor;
    private int[] showChars;
    private int[] boards;
    
    private int level;
    private int exp;
    private int energy;
    private long energyLastUpdate;
   
    private int signInIndex;
    
    private long lastEpochDay;
    private long lastLogin;
    private long createTime;
    
    // Managers
    private final transient CharacterStorage characters;
    private final transient FriendList friendList;
    private final transient BattlePassManager battlePassManager;
    private final transient DatingManager datingManager;
    private final transient StarTowerManager starTowerManager;
    private final transient InstanceManager instanceManager;
    private final transient InfinityTowerManager infinityTowerManager;
    private final transient VampireSurvivorManager vampireSurvivorManager;
    private final transient ScoreBossManager scoreBossManager;
    
    // Referenced data
    private transient Inventory inventory;
    private transient FormationManager formations;
    private transient Mailbox mailbox;
    private transient GachaManager gachaManager;
    private transient PlayerProgress progress;
    private transient StoryManager storyManager;
    private transient QuestManager questManager;
    private transient AchievementManager achievementManager;
    private transient AgentManager agentManager;
    private transient ActivityManager activityManager;
    
    // Extra
    private transient Stack<NetMsgPacket> nextPackages;
    private transient boolean loaded;
    
    @Deprecated // Morphia only
    public Player() {
        // Init player managers
        this.characters = new CharacterStorage(this);
        this.friendList = new FriendList(this);
        this.battlePassManager = new BattlePassManager(this);
        this.datingManager = new DatingManager(this);
        this.starTowerManager = new StarTowerManager(this);
        this.instanceManager = new InstanceManager(this);
        this.infinityTowerManager = new InfinityTowerManager(this);
        this.vampireSurvivorManager = new VampireSurvivorManager(this);
        this.scoreBossManager = new ScoreBossManager(this);
        
        // Init next packages stack
        this.nextPackages = new Stack<>();
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
        this.createTime = Nebula.getCurrentServerTime();
        
        this.name = name;
        this.signature = "";
        this.gender = gender;
        this.headIcon = gender ? 101 : 102;
        this.skinId = 10301;
        this.titlePrefix = 1;
        this.titleSuffix = 2;
        this.honor = new int[3];
        this.showChars = new int[3];
        this.boards = new int[] {410301};
        
        this.level = 1;
        this.energy = 240;
        this.energyLastUpdate = this.createTime;
        
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
    
    public void setSession(GameSession session) {
        // Don't set session if it's the same session
        if (this.session == session) {
            return;
        }
        
        // Cache previous session
        var prevSession = this.session;
        
        // Set session
        this.session = session;
        
        // Clear player reference from the previous session
        if (prevSession != null) {
            prevSession.clearPlayer();
        }
        
        // We cleared session, now remove player from cache
        if (this.session == null) {
            Nebula.getGameContext().getPlayerModule().removeFromCache(this);
        }
    }
    
    public boolean hasSession() {
        return this.session != null;
    }

    public void setLevel(int level) {
        // Set player world class (level)
        this.level = level;
        
        // Save to database
        Nebula.getGameDatabase().update(this, this.getUid(), "level", this.level);
        
        // Trigger achievement
        this.trigger(AchievementCondition.WorldClassSpecific, this.getLevel());
    }
    
    public void setExp(int exp) {
        this.exp = exp;
        Nebula.getGameDatabase().update(this, this.getUid(), "exp", this.exp);
    }

    public void setRemoteToken(String token) {
        // Skip if tokens are the same
        if (this.getRemoteToken() == null) {
            if (token == null) {
                return;
            }
        } else if (this.getRemoteToken().equals(token)) {
            return;
        }
        
        // Set remote token
        this.remoteToken = token;
        
        // Update in database
        Nebula.getGameDatabase().update(this, this.getUid(), "remoteToken", this.remoteToken);
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
        
        // TODO check if title is prefix or suffix
        
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
    
    public boolean setMusic(long id) {
        // Make sure we own the disc
        if (id != 0 && !this.getCharacters().hasDisc((int) id)) {
            return false;
        }
        
        // Set main menu music
        this.music = id;
        
        // Update in database
        Nebula.getGameDatabase().update(this, this.getUid(), "music", this.getMusic());
        
        // Success
        return true;
    }
    
    public boolean setSkin(int skinId) {
        // Skip if we are setting the same skin
        if (this.skinId == skinId) {
            return true;
        }
        
        // Make sure we own this skin
        if (!getInventory().hasSkin(skinId)) {
            return false;
        }
        
        // Set skin
        this.skinId = skinId;
        
        // Update in database
        Nebula.getGameDatabase().update(this, this.getUid(), "skinId", this.getSkinId());
        
        // Success
        return false;
    }

    public boolean setShowChars(RepeatedInt charIds) {
        // Sanity check
        if (charIds.length() > this.getShowChars().length) {
            return false;
        }
        
        // Verify that we have the correct characters
        for (int id : charIds) {
            if (id != 0 && !getCharacters().hasCharacter(id)) {
                return false;
            }
        }
        
        // TODO check duplicates
        
        // Clear
        this.showChars[0] = 0;
        this.showChars[1] = 0;
        this.showChars[2] = 0;
        
        // Set
        for (int i = 0; i < charIds.length(); i++) {
            this.showChars[i] = charIds.get(i);
        }
        
        // Update in database
        Nebula.getGameDatabase().update(this, this.getUid(), "showChars", this.getShowChars());
        
        // Success
        return true;
    }
    
    public boolean setHonor(RepeatedInt honorIds) {
        // Sanity check
        if (honorIds.length() > this.getHonor().length) {
            return false;
        }
        
        // Verify that we have the honor titles
        for (int id : honorIds) {
            // Empty honor title
            if (id == 0) {
                continue;
            }
            
            // Make sure we own the honor title
            if (!getInventory().hasHonor(id)) {
                return false;
            }
            
            // Make sure honor exists and won't crash the client
            var honor = GameData.getHonorDataTable().get(id);
            
            if (honor == null || !honor.isValid()) {
                return false;
            }
        }
        
        // TODO check duplicates
        
        // Clear
        this.honor[0] = 0;
        this.honor[1] = 0;
        this.honor[2] = 0;
        
        // Set
        for (int i = 0; i < honorIds.length(); i++) {
            this.honor[i] = honorIds.get(i);
        }
        
        // Update in database
        Nebula.getGameDatabase().update(this, this.getUid(), "honor", this.getHonor());
        
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
            // Add level
            this.level += 1;
            this.exp -= expRequired;
            
            // Recalculate exp required
            expRequired = this.getMaxExp();
            
            // Set level reward
            this.getQuestManager().getLevelRewards().setBit(this.level);
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
        
        // Save level rewards if we changed it
        if (oldLevel != this.getLevel()) {
            // Update level rewards
            this.getQuestManager().saveLevelRewards();
            
            this.addNextPackage(
                NetMsgId.world_class_reward_state_notify, 
                WorldClassRewardState.newInstance()
                    .setFlag(getQuestManager().getLevelRewards().toBigEndianByteArray())
            );
            
            // Trigger achievement
            this.trigger(AchievementCondition.WorldClassSpecific, this.getLevel());
        }
        
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
        long time = Nebula.getCurrentServerTime();
        
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
        
        // Consume energy
        change = modifyEnergy(-amount, change);
        
        // Trigger quest
        this.trigger(QuestCondition.EnergyDeplete, amount);
        
        // Complete
        return change;
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
    
    // Dailies
    
    public void checkResetDailies() {
        // Sanity check to make sure daily reset isnt being triggered wrong
        if (Nebula.getGameContext().getEpochDays() <= this.getLastEpochDay()) {
            // Fix sign-in index
            // TODO remove later
            if (this.getSignInIndex() <= 0) {
                this.getSignInRewards(false);
            }
            
            // End
            return;
        }
        
        // Check if week has changed (Resets on monday)
        // TODO add a config option
        int curWeek = Utils.getWeeks(this.getLastEpochDay());
        boolean hasWeekChanged = Nebula.getGameContext().getEpochWeeks() > curWeek;
        
        // Check if month was changed
        int curMonth = Utils.getMonths(this.getLastEpochDay());
        boolean hasMonthChanged = Nebula.getGameContext().getEpochMonths() > curMonth;
        
        // Reset dailies
        this.resetDailies(hasWeekChanged, hasMonthChanged);
        
        // Trigger quest/achievement login
        this.trigger(QuestCondition.LoginTotal, 1);

        // Give sign-in rewards
        this.getSignInRewards(hasMonthChanged);
        
        // Update last epoch day
        this.lastEpochDay = Nebula.getGameContext().getEpochDays();
        Nebula.getGameDatabase().update(this, this.getUid(), "lastEpochDay", this.lastEpochDay);
    }
    
    private void getSignInRewards(boolean resetMonthly) {
        // Check monthly reset
        if (resetMonthly) {
            this.signInIndex = 0;
        }
        
        // Get next sign-in index
        int nextSignIn = this.signInIndex + 1;
        int group = Utils.getDaysOfMonth(this.getLastEpochDay());
        
        var data = GameData.getSignInDataTable().get((group << 16) + nextSignIn);
        if (data == null) {
            return;
        }
        
        // Add rewards
        var change = this.getInventory().addItem(data.getItemId(), data.getItemQty());
        
        // Add package
        this.addNextPackage(
            NetMsgId.signin_reward_change_notify, 
            SigninRewardUpdate.newInstance()
                .setIndex(nextSignIn)
                .setSwitch(resetMonthly)
                .setChange(change.toProto())
        );
        
        // Update sign-in index
        this.signInIndex = nextSignIn;
        Nebula.getGameDatabase().update(this, this.getUid(), "signInIndex", this.signInIndex);
    }

    public void resetDailies(boolean resetWeekly, boolean resetMonthly) {
        // Reset daily quests
        this.getQuestManager().resetDailyQuests(resetWeekly);
        this.getBattlePassManager().getBattlePass().resetDailyQuests(resetWeekly);
        
        // Check to reset weeklies
        if (resetWeekly) {
            // Add weekly boss entry item
            int entries = this.getInventory().getResourceCount(GameConstants.WEEKLY_ENTRY_ITEM_ID);
            if (entries < 3) {
                this.getInventory().addItem(GameConstants.WEEKLY_ENTRY_ITEM_ID, 3 - entries);
            }
            
            // Reset weekly tower tickets
            this.getProgress().clearWeeklyTowerTicketLog();
        }
        
        // Check if we need to reset monthly
        if (resetMonthly) {
            // Reset monthly shop purchases
            this.getInventory().resetShopPurchases();
        }
    }
    
    // Trigger quests + achievements
    
    public void trigger(int condition, int progress, int param1, int param2) {
        this.getQuestManager().trigger(condition, progress, param1, param2);
        this.getBattlePassManager().getBattlePass().trigger(condition, progress, param1, param2);
        this.getAchievementManager().trigger(condition, progress, param1, param2);
    }
    
    public void trigger(QuestCondition condition, int progress) {
        this.trigger(condition.getValue(), progress, 0, 0);
    }
    
    public void trigger(QuestCondition condition, int progress, int param1) {
        this.trigger(condition.getValue(), progress, param1, 0);
    }
    
    public void trigger(AchievementCondition condition, int progress) {
        this.trigger(condition.getValue(), progress, 0, 0);
    }
    
    public void trigger(AchievementCondition condition, int progress, int param1, int param2) {
        this.trigger(condition.getValue(), progress, param1, param2);
    }
    
    // Login
    
    private <T extends PlayerManager> T loadManagerFromDatabase(Class<T> cls) {
        var manager = Nebula.getGameDatabase().getObjectByField(cls, "_id", this.getUid());
        
        if (manager != null) {
            manager.setPlayer(this);
            manager.onLoad();
        } else {
            try {
                manager = cls.getDeclaredConstructor(Player.class).newInstance(this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        return manager;
    }
    
    /**
     * Called when the player is loaded from the database
     */
    public void onLoad() {
        // Load from database
        this.getCharacters().loadFromDatabase();
        this.getFriendList().loadFromDatabase();
        this.getStarTowerManager().loadFromDatabase();
        this.getBattlePassManager().loadFromDatabase();
        
        // Load inventory before referenced classes
        if (this.inventory == null) {
            this.inventory = this.loadManagerFromDatabase(Inventory.class);
        }
        this.getInventory().loadFromDatabase();
        
        // Load referenced classes from the database
        this.formations = this.loadManagerFromDatabase(FormationManager.class);
        this.mailbox = this.loadManagerFromDatabase(Mailbox.class);
        this.progress = this.loadManagerFromDatabase(PlayerProgress.class);
        this.gachaManager = this.loadManagerFromDatabase(GachaManager.class);
        this.storyManager = this.loadManagerFromDatabase(StoryManager.class);
        this.questManager = this.loadManagerFromDatabase(QuestManager.class);
        this.achievementManager = this.loadManagerFromDatabase(AchievementManager.class);
        this.agentManager = this.loadManagerFromDatabase(AgentManager.class);
        this.activityManager = this.loadManagerFromDatabase(ActivityManager.class);
        
        // Database fixes
        if (this.showChars == null) {
            this.showChars = new int[3];
            this.save();
        }
        
        // Init activities
        this.getActivityManager().init();
        
        // Load complete
        this.loaded = true;
    }

    public void onCreate() {
        // Send welcome mail
        this.getMailbox().sendWelcomeMail();
    }
    
    public void onLogin() {
        // See if we need to reset dailies
        this.checkResetDailies();
        
        // Fix any broken honor ids
        this.checkBrokenHonor();
        
        // Update activities
        this.getActivityManager().onLogin();
        
        // Update last login time
        this.lastLogin = System.currentTimeMillis();
        Nebula.getGameDatabase().update(this, this.getUid(), "lastLogin", this.getLastLogin());
    }
    
    // Next packages
    
    public boolean hasNextPackages() {
        return this.getNextPackages().size() > 0;
    }
    
    public void addNextPackage(int msgId, ProtoMessage<?> proto) {
        this.getNextPackages().add(new NetMsgPacket(msgId, proto));
    }
    
    // Misc
    
    /**
     * Called AFTER a response is sent to the client
     */
    public void afterResponse() {
        // Check if we need save achievements
        if (this.getAchievementManager().isQueueSave()) {
            this.getAchievementManager().save();
        }
    }
    
    /**
     * Checks the player's honor ids to make sure they don't crash the client
     */
    private void checkBrokenHonor() {
        boolean changed = false;
        
        for (int i = 0; i < this.honor.length; i++) {
            int honorId = this.honor[i];
            
            if (honorId == 0) {
                continue;
            }
            
            // Get honor data
            var honor = GameData.getHonorDataTable().get(honorId);
            
            // Check if honor is valid
            if (honor == null || !honor.isValid()) {
                this.honor[i] = 0;
                changed = true;
            }
        }
        
        // Update in database
        if (changed) {
            Nebula.getGameDatabase().update(this, this.getUid(), "honor", this.getHonor());
        }
    }
    
    // Proto

    public PlayerInfo toProto() {
        PlayerInfo proto = PlayerInfo.newInstance()
                .setServerTs(Nebula.getCurrentServerTime())
                .setSigninIndex(this.getSignInIndex())
                .setTowerTicket(this.getProgress().getTowerTickets())
                .setDailyShopRewardStatus(this.getQuestManager().hasDailyReward())
                .setMusicInfo(this.getMusic())
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
        
        // Set showcase character
        for (int charId : this.getShowChars()) {
            var info = CharShow.newInstance();
            var character = this.getCharacters().getCharacterById(charId);
            
            if (character != null) {
                info.setCharId(character.getCharId())
                    .setLevel(character.getLevel())
                    .setSkin(character.getSkin());
            }
            
            acc.addChars(info);
        }
        
        // Set honor
        for (int honorId : this.getHonor()) {
            var info = HonorInfo.newInstance();
            
            if (honorId != 0) {
                info.setId(honorId);
            }
            
            proto.addHonors(info);
        }
        
        this.getInventory().getHonorList().forEach(proto::addHonorList);
        
        // Set world class
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
        var formations = proto.getMutableFormation();
        for (var f : this.getFormations().getFormations().values()) {
            formations.addInfo(f.toProto());
        }
        
        // Set player states
        var state = proto.getMutableState()
            .setStorySet(this.getStoryManager().hasNew())
            .setFriend(this.getFriendList().hasPendingRequests());
        
        state.getMutableMail()
            .setNew(this.getMailbox().hasNewMail());
        
        state.getMutableBattlePass()
            .setState(this.getBattlePassManager().hasNew() ? 1 : 0);

        state.getMutableAchievement()
            .setNew(this.getAchievementManager().hasNewAchievements());
        
        state.getMutableFriendEnergy()
            .setState(this.getFriendList().hasEnergy());
        state.getMutableMallPackage();
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
        this.getStoryManager().encodePlayerInfo(proto);
        
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
        
        // Quests
        this.getQuestManager().encodePlayerInfo(proto);
        
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
        
        // Add progress
        this.getProgress().encodePlayerInfo(proto);
        
        // Handbook
        proto.addHandbook(this.getCharacters().getCharacterHandbook());
        proto.addHandbook(this.getCharacters().getDiscHandbook());
        proto.addHandbook(this.getStoryManager().getCgHandbook());
        
        // Phone
        var phone = proto.getMutablePhone();
        phone.setNewMessage(this.getCharacters().getNewPhoneMessageCount());
        
        // Agent
        var agentProto = proto.getMutableAgent();
        
        for (var agent : getAgentManager().getAgents().values()) {
            agentProto.addInfos(agent.toProto());
        }
        
        // Activities
        for (var activity : getActivityManager().getActivities().values()) {
            proto.addActivities(activity.toProto());
        }
        
        // Complete
        return proto;
    }
    
    public Friend getFriendProto() {
        var proto = Friend.newInstance()
                .setId(this.getUid())
                .setWorldClass(this.getLevel())
                .setHeadIcon(this.getHeadIcon())
                .setNickName(this.getName())
                .setSignature(this.getSignature())
                .setTitlePrefix(this.getTitlePrefix())
                .setTitleSuffix(this.getTitleSuffix())
                .setLastLoginTime(this.getLastLogin() * 1_000_000L);
        
        for (int charId : this.getShowChars()) {
            var info = CharShow.newInstance()
                    .setCharId(charId)
                    .setLevel(1)                    // TODO
                    .setSkin((charId * 100) + 1);   // TODO
            
            proto.addCharShows(info);
        }
        
        for (int honorId : this.getHonor()) {
            var info = HonorInfo.newInstance()
                    .setId(honorId);
            
            proto.addHonors(info);
        }
        
        return proto;
    }
    
    public Energy getEnergyProto() {
        long nextDuration = Math.max(GameConstants.ENERGY_REGEN_TIME - (Nebula.getCurrentServerTime() - getEnergyLastUpdate()), 1);
        
        var proto = Energy.newInstance()
                .setUpdateTime(this.getEnergyLastUpdate())
                .setNextDuration(nextDuration)
                .setPrimary(this.getEnergy())
                .setIsPrimary(true);
        
        return proto;
    }
    
}