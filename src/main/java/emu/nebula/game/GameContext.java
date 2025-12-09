package emu.nebula.game;

import java.time.Instant;
import java.time.LocalDate;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import emu.nebula.GameConstants;
import emu.nebula.Nebula;
import emu.nebula.game.activity.ActivityModule;
import emu.nebula.game.ban.BanModule;
import emu.nebula.game.gacha.GachaModule;
import emu.nebula.game.player.PlayerModule;
import emu.nebula.game.scoreboss.ScoreBossModule;
import emu.nebula.game.tutorial.TutorialModule;
import emu.nebula.net.GameSession;
import emu.nebula.util.Utils;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import lombok.Getter;

@Getter
public class GameContext implements Runnable {
    private final Object2ObjectMap<String, GameSession> sessions;
    
    // Modules
    private final PlayerModule playerModule;
    private final GachaModule gachaModule;
    private final TutorialModule tutorialModule;
    private final ActivityModule activityModule;
    private final ScoreBossModule scoreBossModule;
    private final BanModule banModule;
    
    // Game loop
    private final ScheduledExecutorService scheduler;
    
    // Daily
    private long epochDays;
    private int epochWeeks;
    private int epochMonths;
    
    public GameContext() {
        this.sessions = new Object2ObjectOpenHashMap<>();
        
        // Setup game modules
        this.playerModule = new PlayerModule(this);
        this.gachaModule = new GachaModule(this);
        this.tutorialModule = new TutorialModule(this);
        this.activityModule = new ActivityModule(this);
        this.scoreBossModule = new ScoreBossModule(this);
        this.banModule = new BanModule(this);
        
        // Run game loop
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.scheduler.scheduleAtFixedRate(this, 0, 1, TimeUnit.SECONDS);
    }
    
    public synchronized GameSession getSessionByToken(String token) {
        return sessions.get(token);
    }
    
    public synchronized void addSession(GameSession session) {
        this.sessions.put(session.getToken(), session);
    }

    public synchronized void generateSessionToken(GameSession session) {
        // Remove token
        if (session.getToken() != null) {
            this.sessions.remove(session.getToken());
        }
        
        // Generate token
        String token = null;
        
        do {
            token = session.generateToken();
        } while (this.getSessions().containsKey(token));
        
        // Register session
        this.sessions.put(session.getToken(), session);
    }
    
    // TODO add timeout to config
    public synchronized void cleanupInactiveSessions() {
        var it = this.getSessions().entrySet().iterator();
        
        int time = Nebula.getConfig().getServerOptions().sessionTimeout;
        long timeout = System.currentTimeMillis() - TimeUnit.SECONDS.toMillis(time);
        
        while (it.hasNext()) {
            var session = it.next().getValue();
            
            if (timeout > session.getLastActiveTime() || session.isRemove()) {
                // Remove from session map
                it.remove();
                
                // Clear player
                session.clearPlayer();
            }
        }
    }

    @Override
    public void run() {
        // Check daily - Update epoch days
        long offset = Nebula.getConfig().getServerOptions().getDailyResetHour() * -3600;
        var instant = Instant.now().plusSeconds(offset);
        var date = LocalDate.ofInstant(instant, GameConstants.UTC_ZONE);
        
        // Update epoch days
        long lastEpochDays = this.epochDays;
        this.epochDays = date.toEpochDay();
        
        // Check if the day was changed
        if (this.epochDays > lastEpochDays) {
            // Update epoch weeks/months
            this.epochWeeks = Utils.getWeeks(this.epochDays);
            this.epochMonths = Utils.getMonths(this.epochDays);
            
            // Reset dailies for players
            this.resetDailies();
        }
        
        // Clean up any inactive sessions
        this.cleanupInactiveSessions();
    }

    /**
     * Resets the daily missions/etc for all players on the server
     */
    public synchronized void resetDailies() {
        for (var session : this.getSessions().values()) {
            // Cache
            var player = session.getPlayer();
            
            // Skip if session doesn't have a player
            if (player == null) {
                continue;
            }
            
            //
            player.checkResetDailies();
        }
    }
}
