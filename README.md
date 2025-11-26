# Nebula

A work in progress game server emulator for a certain anime game.

For any extra support, questions, or discussions, check out our [Discord](https://discord.gg/cskCWBqdJk).

### Notable features
- Basic profile features
- Character system implemented (except for affinity)
- Inventory/Discs working
- Energy system
- Mail system
- Story (untested)
- Daily quests
- Battle pass
- Gacha
- Friend system (sending energy not implemented)
- Shop (using only in-game currency)
- Commissions
- Heartlink (missing advanced affinity related features)
- Monoliths (completeable but many other features missing)
- Bounty Trials
- Menance Arena
- Proving grounds
- Catacylsm Survivor (talents not fully working, score not calculated properly)
- Boss Blitz

### Not implemented
- Achievements
- Events

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
import System;
import System.Windows.Forms;
import Fiddler;
import System.Text.RegularExpressions;

class Handlers
{
    static function OnBeforeRequest(oS: Session) {
        if (oS.host.EndsWith(".yostarplat.com") || oS.host.EndsWith(".stellasora.global")) {
            oS.oRequest.headers.UriScheme = "http";
            oS.host = "localhost"; // This can also be replaced with another IP address.
        }
    }
};
```

4. If `autoCreateAccount` is set to true in the config, then you can skip this step. Otherwise, type `/account create [account email]` in the server console to create an account.
5. Login with your account name, the code field is ignored by the server and can be set to anything.

### Server commands
Server commands need to be run in the server console OR in the signature edit menu of your profile.

```
!account {create | delete} [email] (reserved player uid) = Creates or deletes an account.
!char [all | {characterId}] lv(level) a(ascension) s(skill level) t(talent level) f(affinity level) = Changes the properties of the targeted characters.
!clean [all | {id} ...] [items|resources] = Removes items/resources from the targeted player.
!disc [all | {discId}] lv(level) a(ascension) c(crescendo level) = Changes the properties of the targeted discs.
!give [item id] x[amount] = Gives the targeted player an item through the mail.
!giveall [characters | discs | materials] = Gives the targeted player items.
!mail = Sends the targeted player a system mail.
!reload = Reloads the server config.
```
