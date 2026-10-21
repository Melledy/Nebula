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
- Story
- Daily quests
- Battle pass
- Gacha
- Friend system
- Shop (only in-game currency supported)
- Commissions
- Heartlink
- Achievements
- Tower (dorm system)
- Monoliths
- Bounty Trials
- Menance Arena
- Proving Grounds
- Catacylsm Survivor (talents not fully working)
- Boss Blitz
- Shadow Siege
- Events (Only tower defense and trials. Other events may work)

### Supported regions

Nebula supports the global PC client by default. If you want to switch regions, you need to change the `region` field in the Nebula config.

Current supported regions (PC): `GLOBAL`, `KR`, `JP`, `TW`, `CN`

### Updating

You may need to change the data version when switching regions OR if your game updates. The `customDataVersion` field in the config should match the data version of your client, which is usually the last number of your client's version string (top left of your login screen). Example: 1.0.0.42 = data version 42.

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

Server commands can be run from the server console or from the signature edit menu of your profile. You can prefix a command with either `!` or `/`.

Commands that modify a player need a target: run them in-game, or append `@<uid>` to target a player from the console.

| Command | Aliases | Description |
| --- | --- | --- |
| `!account` | | Create an account (optionally reserving a player UID), delete an account, or list all accounts.<br>`!account create <email> [reserved uid]`<br>`!account delete <email>`<br>`!account list` |
| `!arena` | `setarena` | Set the Menace Arena floor and level for a player, or clear their progress. Run without arguments to view progress. Requires a relog.<br>`!arena <element> <floor> <level> [@uid]`<br>`!arena clear`<br>Elements: `generic`, `ignis`, `ventus`, `lux`, `aqua`, `terra`, `umbra` (or `1`-`7`). |
| `!autobuild` | `ab` | Generate a record for a player, auto-filling characters, discs, potentials, and sub-note skills to reach a target record level or score. Uses record level 40 if none is given.<br>`!autobuild [ids...] lv(level) s(score) [-nn] [-np]`<br>`-nn` skips sub-note skills and `-np` skips potentials. |
| `!ban` | | Ban a player or IP address.<br>`!ban {all \| ip \| uid} <uid \| ip> [end timestamp] [reason]`<br>`all` bans both the UID and its IP, `uid` only the UID, and `ip` only the IP. The end timestamp is in milliseconds; omit it for a permanent ban. |
| `!battlepass` | `bp` | Set a player's battle pass mode and level (maximum level 50).<br>`!battlepass [free \| premium] lv(level)` |
| `!build` | `b`, `record`, `r` | Create a record from item IDs. Requires exactly 3 characters and 3-6 discs; potential levels can be set with `id:level`, and a preset code can be used instead of IDs.<br>`!build [character ids...] [disc ids...] [potential ids...] [sub-note skill ids...]` |
| `!character` | `c`, `char` | Change a character's level, ascension, skill, talent, and affinity.<br>`!character [all \| character ids...] lv(level) a(ascension) s(skill level) t(talent level) f(affinity level)`<br>`all` applies the changes to every owned character. |
| `!clean` | `cl`, `clear` | Remove items and/or resources from a player.<br>`!clean [all \| item ids...] [items \| resources]`<br>`all` removes everything; `items` or `resources` limits the cleanup to one type. |
| `!disc` | `d` | Change a disc's level, ascension, and crescendo level.<br>`!disc [all \| disc ids...] lv(level) a(ascension) c(crescendo level)`<br>`all` applies the changes to every owned disc. |
| `!emblem` | `e`, `gem` | Set the four attributes of an emblem in a character's emblem slot, creating or replacing the gem as needed.<br>`!emblem <character id> <emblem slot> <attr id> <attr id> <attr id> <attr id>` |
| `!give` | `g`, `item` | Send items to a player through the mail.<br>`!give <item ids...> x(amount)`<br>Defaults to 1 of each item. |
| `!giveall` | `ga` | Give a player all materials, discs, characters, or paid skins.<br>`!giveall [materials \| discs \| characters \| skins] lv(level) a(ascension) s(skill level) t(talent/crescendo level)`<br>Property values are applied to newly added characters and discs. |
| `!help` | | List every command available to you.<br>`!help`<br>This is very spammy in-game. |
| `!level` | `setlevel`, `l` | Set a player's level.<br>`!level <level>` |
| `!mail` | `m` | Send a system mail with optional attachments. The subject and body must be wrapped in quotes.<br>`!mail "subject" "body" [itemId xQty \| itemId:qty ...]` |
| `!reload` | | Reload the server config and the game patch list.<br>`!reload` |
| `!remote` | | Generate a remote API token for a player, or clear it when remote services are disabled.<br>`!remote` |
| `!status` | | Show server runtime status: uptime, CPU, memory, disk, and player count.<br>`!status [@uid]`<br>Add `@uid` to verify that a player is online. |
| `!unban` | | Unban a player or IP address.<br>`!unban {all \| ip \| uid} <uid \| ip>`<br>`all` unbans both the UID and its IP, `uid` only the UID, and `ip` only the IP. |
