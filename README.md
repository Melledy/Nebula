# Nebula

A work in progress game server emulator for a certain anime game.

For any extra support, questions, or discussions, check out our [Discord](https://discord.gg/cskCWBqdJk).

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
- Friend system (sending energy not implemented)
- Shop (only in-game currency supported)
- Commissions
- Heartlink
- Achievements
- Monoliths (completeable but many other features missing)
- Bounty Trials
- Menance Arena
- Proving grounds
- Catacylsm Survivor (talents not fully working)
- Boss Blitz

### Not implemented
- Events

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
1. Open your system terminal, and compile the server with `./gradlew jar`
2. Create a folder named `resources` in your server directory
3. Download the `bin`, `language` folders from a repository with [datamined game data](https://github.com/Hiro420/StellaSoraData) and place them into your resources folder.
4. Run the server with `java -jar Nebula.jar` from your system terminal. This server comes with a built-in internal MongoDB server for its database, so no Mongodb installation is required. However, it is highly recommended to install Mongodb anyway. 

### Connecting with the client (Fiddler method)
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
!char [all | {characterId}] lv(level) a(ascension) s(skill level) t(talent level) f(affinity level) = Changes the properties of the targeted characters.
!clean [all | {id} ...] [items|resources] = Removes items/resources from the targeted player.
!disc [all | {discId}] lv(level) a(ascension) c(crescendo level) = Changes the properties of the targeted discs.
!give [item id] x[amount] = Gives the targeted player an item through the mail.
!giveall [characters | discs | materials] = Gives the targeted player items.
!level (level) = Sets the player level
!mail = Sends the targeted player a system mail.
!reload = Reloads the server config.
```
