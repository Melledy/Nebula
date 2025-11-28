package emu.nebula;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import emu.nebula.command.CommandManager;
import emu.nebula.data.ResourceLoader;
import emu.nebula.database.DatabaseManager;
import emu.nebula.game.GameContext;
import emu.nebula.net.PacketHelper;
import emu.nebula.plugin.PluginManager;
import emu.nebula.server.HttpServer;
import emu.nebula.util.AeadHelper;
import emu.nebula.util.Handbook;
import emu.nebula.util.JsonUtils;
import lombok.Getter;

public class Nebula {
    private static final Logger log = LoggerFactory.getLogger(Nebula.class);
    
    // Config
    private static final File configFile = new File("./config.json");
    @Getter private static Config config;
    
    // Database
    @Getter private static DatabaseManager accountDatabase;
    @Getter private static DatabaseManager gameDatabase;
    
    // Server
    @Getter private static HttpServer httpServer;
    @Getter private static HttpServer gameServer; // TODO
    @Getter private static ServerType serverType = ServerType.BOTH;
    
    @Getter private static GameContext gameContext;
    @Getter private static CommandManager commandManager;
    @Getter private static PluginManager pluginManager;
    
    public static void main(String[] args) {
        // Load config + keys first
        Nebula.loadConfig();
        AeadHelper.loadKeys();
        
        // Start Server
        Nebula.getLogger().info("Starting Nebula " + getJarVersion());
        Nebula.getLogger().info("Git hash: " + getGitHash());
        Nebula.getLogger().info("Game version: " + GameConstants.getGameVersion());
        
        boolean generateHandbook = true;
        
        // Load plugin manager
        Nebula.pluginManager = new PluginManager();

        try {
            Nebula.getPluginManager().loadPlugins();
        } catch (Exception exception) {
            Nebula.getLogger().error("Unable to load plugins.", exception);
        }
        
        // Parse arguments
        for (String arg : args) {
            switch (arg) {
                case "-login":
                    serverType = ServerType.LOGIN;
                    break;
                case "-game":
                    serverType = ServerType.GAME;
                    break;
                case "-nohandbook":
                case "-skiphandbook":
                    generateHandbook = false;
                    break;
                case "-database":
                    // Database only
                    DatabaseManager.startInternalMongoServer(Nebula.getConfig().getInternalMongoServer());
                    Nebula.getLogger().info("Running local Mongo server at " + DatabaseManager.getServer().getConnectionString());
                    return;
            }
        }
        
        // Skip these if we are only running the http server in dispatch mode
        if (serverType.runGame()) {
            // Load resources
            ResourceLoader.loadAll();
            // Generate handbook
            if (generateHandbook) {
                Handbook.generate();
            }
            // Cache proto methods
            PacketHelper.cacheProtos();
        }

        try {
            // Start Database(s)
            Nebula.initDatabases();
        } catch (Exception exception) {
            Nebula.getLogger().error("Unable to start the database(s).", exception);
        }
        
        // Start game context
        Nebula.gameContext = new GameContext();
        Nebula.commandManager = new CommandManager();
        
        // Start servers
        try {
            // Always run http server as it is needed by for login and game server
            httpServer = new HttpServer(serverType);
            httpServer.start();
        } catch (Exception exception) {
            Nebula.getLogger().error("Unable to start the HTTP server.", exception);
        }
        
        // Enable plugins
        Nebula.getPluginManager().enablePlugins();
        
        // Start console
        Nebula.startConsole();
    }

    public static Logger getLogger() {
        return log;
    }
    
    // Database

    private static void initDatabases() {
        if (Nebula.getConfig().useSameDatabase) {
            // Setup account and game database
            accountDatabase = new DatabaseManager(Nebula.getConfig().getAccountDatabase(), serverType);
            // Optimization: Dont run a 2nd database manager if we are not running a gameserver
            if (serverType.runGame()) {
                gameDatabase = accountDatabase;
            }
        } else {
            // Run separate databases
            accountDatabase = new DatabaseManager(Nebula.getConfig().getAccountDatabase(), ServerType.LOGIN);
            // Optimization: Dont run a 2nd database manager if we are not running a gameserver
            if (serverType.runGame()) {
                gameDatabase = new DatabaseManager(Nebula.getConfig().getGameDatabase(), ServerType.GAME);
            }
        }
    }
    
    // Config

    public static void loadConfig() {
        // Load from file
        try (FileReader file = new FileReader(configFile)) {
            Nebula.config = JsonUtils.loadToClass(file, Config.class);
        } catch (Exception e) {
            // Ignored
        }
        
        // Sanity check
        if (Nebula.getConfig() == null) {
            Nebula.config = new Config();
        }
        
        // Save config
        Nebula.saveConfig();
    }

    public static void saveConfig() {
        try (FileWriter file = new FileWriter(configFile)) {
            Gson gson = new GsonBuilder()
                    .setDateFormat("dd-MM-yyyy hh:mm:ss")
                    .setPrettyPrinting()
                    .serializeNulls()
                    .create();
            
            file.write(gson.toJson(config));
        } catch (Exception e) {
            getLogger().error("Config save error");
        }
    }
    
    // Build Config
    
    private static String getJarVersion() {
        // Safely get the build config class without errors even if it hasnt been generated yet
        try {
            Class<?> buildConfig = Class.forName(Nebula.class.getPackageName() + ".BuildConfig");
            return buildConfig.getField("VERSION").get(null).toString();
        } catch (Exception e) {
            // Ignored
        }
        
        return "";
    }

    public static String getGitHash() {
        // Use a string builder in case one of the build config fields are missing
        StringBuilder builder = new StringBuilder();
        
        // Safely get the build config class without errors even if it hasnt been generated yet
        try {
            SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Class<?> buildConfig = Class.forName(Nebula.class.getPackageName() + ".BuildConfig");
            
            String hash = buildConfig.getField("GIT_HASH").get(null).toString();
            builder.append(hash);
            
            String timestamp = buildConfig.getField("GIT_TIMESTAMP").get(null).toString();
            long time = Long.parseLong(timestamp) * 1000;
            builder.append(" (" + sf.format(new Date(time)) + ")");
        } catch (Exception e) {
            // Ignored
        }
        
        if (builder.isEmpty()) {
            return "UNKNOWN";
        } else {
            return builder.toString();
        }
    }
    
    // Utils

    /**
     * Returns the current server time in seconds
     */
    public static long getCurrentTime() {
        return System.currentTimeMillis() / 1000;
    }
    
    // Console

    private static void startConsole() {
        String input;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
            while ((input = br.readLine()) != null) {
                var result = Nebula.getCommandManager().invoke(null, input);
                Nebula.getLogger().info(result.getMessage());
            }
        } catch (Exception e) {
            Nebula.getLogger().error("Console error:", e);
        }
    }
    
    // Server enums

    public enum ServerType {
        LOGIN       (0x1),
        GAME        (0x2),
        BOTH        (0x3);

        private final int flags;

        ServerType(int flags) {
            this.flags = flags;
        }

        public boolean runLogin() {
            return (this.flags & 0x1) == 0x1;
        }

        public boolean runGame() {
            return (this.flags & 0x2) == 0x2;
        }
    }
}
