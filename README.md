# Nebula

A work in progress game server emulator for a certain anime game. Most features are implemented.

For any extra support, questions, or discussions, check out our [Discord](https://discord.gg/cskCWBqdJk).

Latest nightly compiled server jar: [Nightly](https://nightly.link/Melledy/Nebula/workflows/gradle/main/Nebula-Nightly.zip)

### Notable features
- Basic profile features
- Character system
- Inventory/Discs working
- Energy system
- Mail system
- Story (untested)
- Daily quests
- Battle pass
- Gacha
- Friend system
- Shop (only in-game currency supported)
- Commissions
- Heartlink
- Achievements
- Monoliths (research quests not implemented)
- Bounty Trials
- Menance Arena
- Proving Grounds
- Catacylsm Survivor (talents not fully working)
- Boss Blitz
- Events (Only tower defense and trials)

### Supported regions

Nebula supports the global PC client by default. If you want to switch regions, you need to change the `region` field in the Nebula config.

Current supported regions (PC): `GLOBAL`, `KR`, `JP`, `TW`, `CN`

You may need to change the data version when switching regions. The `customDataVersion` field should match the the data version of your client, which is usually the last number of your client's version string (top left of your login screen). Example: 1.0.0.42 = data version 42.

# Running the server and client

### Prerequisites
* [Java 21](https://www.oracle.com/java/technologies/javase/jdk21-archive-downloads.html)

### Recommended
* [MongoDB 4.0+](https://www.mongodb.com/try/download/community)

### Compiling the server
1. Open your system terminal, and compile the server with `./gradlew jar` (If you downloaded the server jar from the Nightly link, you can skip this step)
2. Create a folder named `resources` in your server directory
3. Download the `bin`, `language` folders from a repository with [datamined game data](https://github.com/Hiro420/StellaSoraData) and place them into your resources folder.
4. Run the server with `java -jar Nebula.jar` from your system terminal. This server comes with a built-in internal MongoDB server for its database, so no Mongodb installation is required. However, it is highly recommended to install Mongodb anyway. 

### Connecting with the client
You can do this either via [mitmproxy](https://www.mitmproxy.org/) or [Fiddler Classic](https://www.telerik.com/fiddler/fiddler-classic).

#### Using mitmproxy (recommended)
> [!IMPORTANT]
> If you intend to connect using clients other than global, you may need to modify `mitmproxy/proxy.py` to also redirect their appropriate endpoints as well.

1. Download and install mitmproxy from [here](https://www.mitmproxy.org/) (do **NOT** get the Microsoft Store version)
2. Navigate to the `mitmproxy` directory and run `proxy.bat`
3. Navigate to `%userprofile%\.mitmproxy` and import `mitmproxy-ca-cert.cer` to **Local Machine**, place it under the **Trusted Root Certification Authorities** certificate store
4. You may now launch the game

If Nebula is exposed on another port other than `80`, open `mitmproxy/proxy.py` with your favorite text editor and modify the `SERVER_PORT` field to the appropriate port.

#### Using Fiddler Classic
1. **Log in with the client to an official server at least once to download game data.**
2. Install and have [Fiddler Classic](https://www.telerik.com/fiddler) running.
3. Copy and paste the following code into the Fiddlerscript tab of Fiddler Classic. Remember to save the fiddler script after you copy and paste it:

```
import Fiddler;

class Handlers
{
    static var list = [
        ".yostarplat.com",
        ".stellasora.global",
        ".stellasora.kr",
        ".stellasora.jp",
        ".stargazer-games.com",
        ".yostar.cn"
    ];

    static function OnBeforeRequest(oS: Session) {
        for (var i = 0; i < list.length; i++) {
            if (oS.host.EndsWith(list[i])) {
                oS.oRequest.headers.UriScheme = "http";
                oS.host = "localhost"; // This can also be replaced with another IP address
            }
        }
    }
};
```

4. If `autoCreateAccount` is set to true in the config, then you can skip this step. Otherwise, type `/account create [account email]` in the server console to create an account.
5. Login with your account email, the code field is ignored by the server and can be set to anything.

### Server commands
Server commands need to be run in the server console OR in the signature edit menu of your profile.

```
!account {create | delete} [email] (reserved player uid) = Creates or deletes an account.
!battlepass [free | premium] lv(level) = Modifies the targeted player's battle pass
!char [all | {characterId}] lv(level) a(ascension) s(skill level) t(talent level) f(affinity level) = Changes the properties of the targeted characters.
!clean [all | {id} ...] [items|resources] = Removes items/resources from the targeted player.
!disc [all | {discId}] lv(level) a(ascension) c(crescendo level) = Changes the properties of the targeted discs.
!give [item id] x[amount] = Gives the targeted player an item through the mail.
!giveall [characters | discs | materials] = Gives the targeted player items.
!help = Displays a list of available commands. (Very spammy in-game)
!level (level) = Sets the player level
!mail "subject" "body" [itemId xQty | itemId:qty ...] = Sends the targeted player a system mail.
!reload = Reloads the server config.
!remote = Creates a player token for remote api usage
```
