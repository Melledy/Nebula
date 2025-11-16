package emu.nebula.game.player;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.mongodb.client.model.Filters;

import emu.nebula.Nebula;
import emu.nebula.game.GameContext;
import emu.nebula.game.GameContextModule;
import emu.nebula.game.account.Account;
import emu.nebula.game.agent.AgentManager;
import emu.nebula.game.battlepass.BattlePass;
import emu.nebula.game.character.Character;
import emu.nebula.game.character.GameDisc;
import emu.nebula.game.formation.FormationManager;
import emu.nebula.game.friends.Friendship;
import emu.nebula.game.gacha.GachaManager;
import emu.nebula.game.inventory.GameItem;
import emu.nebula.game.inventory.GameResource;
import emu.nebula.game.inventory.Inventory;
import emu.nebula.game.mail.Mailbox;
import emu.nebula.game.quest.QuestManager;
import emu.nebula.game.story.StoryManager;
import emu.nebula.game.tower.StarTowerBuild;
import emu.nebula.net.GameSession;
import emu.nebula.proto.ScoreBossRank;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

public class PlayerModule extends GameContextModule {
    private final Int2ObjectMap<Player> cachedPlayers;
    private final Object2ObjectMap<String, Player> cachedPlayersByAccount;

    public PlayerModule(GameContext gameContext) {
        super(gameContext);

        this.cachedPlayers = new Int2ObjectOpenHashMap<>();
        this.cachedPlayersByAccount = new Object2ObjectOpenHashMap<>();
    }

    public Int2ObjectMap<Player> getCachedPlayers() {
        return cachedPlayers;
    }
    
    private void addToCache(Player player) {
        this.cachedPlayers.put(player.getUid(), player);
        this.cachedPlayersByAccount.put(player.getAccountUid(), player);
    }
    
    public void removeFromCache(Player player) {
        this.cachedPlayers.remove(player.getUid());
        this.cachedPlayersByAccount.remove(player.getAccountUid());
    }

    /**
     * Returns a player object that has been previously cached. Returns null if the player isnt in the cache.
     * @param uid User id of the player
     * @return
     */
    public synchronized Player getCachedPlayerByUid(int uid) {
        return getCachedPlayers().get(uid);
    }
    
    /**
     * Returns a player object with the given uid. Returns null if the player doesnt exist.
     * Warning: Does NOT cache or load the playerdata if the player was loaded from the database.
     * @param uid User id of the player
     * @return
     */
    public synchronized Player getPlayer(int uid) {
        // Get player from cache
        Player player = this.cachedPlayers.get(uid);

        if (player == null) {
            // Retrieve player object from database if its not there
            player = Nebula.getGameDatabase().getObjectByUid(Player.class, uid);
        }

        return player;
    }
    
    /**
     * Returns a player object with the given account. Returns null if the player doesnt exist.
     * @param uid User id of the player
     * @return
     */
    public synchronized Player loadPlayer(Account account) {
        // Get player from cache
        Player player = this.cachedPlayersByAccount.get(account.getUid());

        if (player == null) {
            // Retrieve player object from database if its not there
            player = Nebula.getGameDatabase().getObjectByField(Player.class, "accountUid", account.getUid());

            if (player != null) {
                // Load player
                player.onLoad();

                // Put in cache
                this.addToCache(player);
            }
        }

        return player;
    }

    /**
     * Creates a player with the specified user id.
     * @param userId
     * @return
     */
    public synchronized Player createPlayer(GameSession session, String name, boolean gender) {
        // Make sure player doesnt already exist
        if (Nebula.getGameDatabase().checkIfObjectExists(Player.class, "accountUid", session.getAccount().getUid())) {
            return null;
        }
        
        // Limit name length
        if (name.length() > 20) {
            name = name.substring(0, 19);
        }

        // Create player and save to db
        var player = new Player(session.getAccount(), name, gender);
        player.onLoad();
        player.save();
        
        // Send welcome mail
        player.getMailbox().sendWelcomeMail();
        
        // Put in player cache
        this.addToCache(player);
        
        // Set player for session
        session.setPlayer(player);

        return player;
    }
    
    /**
     * Deletes a player from the database. The player must be offline.
     * @param uid
     * @return
     */
    public synchronized boolean deletePlayer(int uid) {
        // Make sure player is not online when we are deleting the player
        Player player = this.getCachedPlayerByUid(uid);
        if (player != null) {
            return false;
        }
        
        // Get player from database
        player = Nebula.getGameDatabase().getObjectByUid(Player.class, uid);
        if (player == null) {
            return false;
        }
        
        // Cache filter objects so we can reuse it for our delete queries
        var multiFilter = Filters.eq("playerUid", uid);
        var idFilter = Filters.eq("_id", uid);
        
        // Get datastore
        var datastore = Nebula.getGameDatabase().getDatastore();
        
        // Delete data from collections
        datastore.getCollection(Character.class).deleteMany(multiFilter);
        datastore.getCollection(GameDisc.class).deleteMany(multiFilter);
        datastore.getCollection(GameItem.class).deleteMany(multiFilter);
        datastore.getCollection(GameResource.class).deleteMany(multiFilter);
        datastore.getCollection(StarTowerBuild.class).deleteMany(multiFilter);

        datastore.getCollection(Inventory.class).deleteOne(idFilter);
        datastore.getCollection(FormationManager.class).deleteOne(idFilter);
        datastore.getCollection(Mailbox.class).deleteOne(idFilter);
        datastore.getCollection(PlayerProgress.class).deleteOne(idFilter);
        datastore.getCollection(GachaManager.class).deleteOne(idFilter);
        datastore.getCollection(StoryManager.class).deleteOne(idFilter);
        datastore.getCollection(QuestManager.class).deleteOne(idFilter);
        datastore.getCollection(AgentManager.class).deleteOne(idFilter);
        
        datastore.getCollection(BattlePass.class).deleteOne(idFilter);
        datastore.getCollection(ScoreBossRank.class).deleteOne(idFilter);
        
        // Delete friendships
        datastore.getCollection(Friendship.class).deleteMany(Filters.or(Filters.eq("playerUid", uid), Filters.eq("friendUid", uid)));
        
        // Finally delete the player
        datastore.getCollection(Player.class).deleteOne(idFilter);
        
        // Success
        return true;
    }
    
    /**
     * Returns a list of recent players that have logged on
     * @param player Player that requested this
     */
    public synchronized List<Player> getRandomPlayerList(Player player) {
        List<Player> list = getCachedPlayers().values().stream()
                .filter(p -> p != player)
                .limit(10)
                .collect(Collectors.toList());
        
        Collections.shuffle(list);
        
        return list.stream().toList();
    }
}
