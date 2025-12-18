package emu.nebula;

import java.util.List;
import java.util.Set;

import emu.nebula.game.inventory.ItemParam;
import lombok.Getter;

@Getter
public class Config {
    public DatabaseInfo accountDatabase = new DatabaseInfo();
    public DatabaseInfo gameDatabase = new DatabaseInfo();
    public InternalMongoInfo internalMongoServer = new InternalMongoInfo();
    public boolean useSameDatabase = true;

    public KeystoreInfo keystore = new KeystoreInfo();

    public HttpServerConfig httpServer = new HttpServerConfig(80);
    public GameServerConfig gameServer = new GameServerConfig(80);

    public ServerOptions serverOptions = new ServerOptions();
    public ServerRates serverRates = new ServerRates();
    public LogOptions logOptions = new LogOptions();
    public RemoteCommand remoteCommand = new RemoteCommand();

    public int customDataVersion = 0;
    public String region = "global";
    public String language = "en_US";
    
    public String resourceDir = "./resources";
    public String webFilesDir = "./web";
    public String patchListPath = "./patchlist.json";

    @Getter
    public static class DatabaseInfo {
        public String uri = "mongodb://localhost:27017";
        public String collection = "nebula";
        public boolean useInternal = true;
    }

    @Getter
    public static class InternalMongoInfo {
        public String address = "localhost";
        public int port = 27017;
        public String filePath = "database.mv";
    }

    @Getter
    public static class KeystoreInfo {
        public String path = "./keystore.p12";
        public String password = "";
    }

    @Getter
    private static class ServerConfig {
        public boolean useSSL = false;
        public String bindAddress = "0.0.0.0";
        public int bindPort;
        public String publicAddress = "127.0.0.1"; // Will return bindAddress if publicAddress is null
        public Integer publicPort; // Will return bindPort if publicPort is null

        public ServerConfig(int port) {
            this.bindPort = port;
        }

        public String getPublicAddress() {
            if (this.publicAddress != null && !this.publicAddress.isEmpty()) {
                return this.publicAddress;
            }

            return this.bindAddress;
        }

        public int getPublicPort() {
            if (this.publicPort != null && this.publicPort != 0) {
                return this.publicPort;
            }

            return this.bindPort;
        }

        public String getDisplayAddress() {
            return (useSSL ? "https" : "http") + "://" + getPublicAddress() + ":" + getPublicPort();
        }
    }

    @Getter
    public static class HttpServerConfig extends ServerConfig {

        public HttpServerConfig(int port) {
            super(port);
        }
    }

    @Getter
    public static class GameServerConfig extends ServerConfig {

        public GameServerConfig(int port) {
            super(port);
        }
    }

    @Getter
    public static class ServerOptions {
        // Default permissions for accounts. By default, all commands are allowed. Reccomended to change if making a public server.
        public Set<String> defaultPermissions = Set.of("*");
        // Automatically creates an account when a player logs in for the first time on a new email.
        public boolean autoCreateAccount = true;
        // Skips the intro cinematics/stage when starting a new account.
        public boolean skipIntro = false;
        // Unlocks all instances (Monolith, Bounty Trials, etc) for players to enter without needing to do the previous levels.
        public boolean unlockInstances = true;
        // Unlocks all story CGs to use in the showcase
        public boolean unlockAllStoryCGs = false;
        // How long to wait (in seconds) after the last http request from a session before removing it from the server.
        public int sessionTimeout = 300;
        // The offset hour for when daily quests are refreshed in UTC. Example: "dailyResetHour = 4" means dailies will be refreshed at UTC+4 12:00 AM every day.
        public int dailyResetHour = 0;
        // Leaderboard for Boss Blitz refresh time in seconds.
        public int leaderboardRefreshTime = 60;
        // The welcome mail to send when a player is created. Set to null to disable.
        public WelcomeMail welcomeMail = new WelcomeMail();
    }

    @Getter
    public static class ServerRates {
        public double exp = 1.0;
    }

    @Getter
    public static class LogOptions {
        public boolean commands = true;
        public boolean packets = false;
        public boolean httpDebug = false;
    }

    @Getter
    public static class RemoteCommand {
        public boolean useRemoteServices = false;
        public String serverAdminKey = "HJHASDPIIQWEASDHHAN";
    }

    @Getter
    public static class WelcomeMail {
        public String title;
        public String sender;
        public String content;
        public List<ItemParam> attachments;

        public WelcomeMail() {
            this.title = "Welcome to a Nebula server";
            this.sender = "Server";
            this.content = "Welcome to Nebula! Please take these items as a starter gift.";
            this.attachments = List.of(
                new ItemParam(86009, 1),
                new ItemParam(86002, 1),
                new ItemParam(1, 1_000_000),
                new ItemParam(2, 30_000));
        }
    }

}
