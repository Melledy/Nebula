package emu.nebula.game.ban;

import emu.nebula.Nebula;
import emu.nebula.game.player.Player;
import emu.nebula.game.GameContext;
import emu.nebula.game.GameContextModule;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BanModule extends GameContextModule {
    private final Map<String, BanInfo> cachedIpBans = new ConcurrentHashMap<>();
    private final Map<Integer, BanInfo> cachedPlayerBans = new ConcurrentHashMap<>();
    
    public BanModule(GameContext context) {
        super(context);
    }
    
    /**
     * Ban a player by UID
     * @param uid Player UID
     * @param endTime Ban expiration time (0 = permanent)
     * @param isBanIp Ban Ip
     * @param reason Ban reason
     * @param bannedBy Who banned the player
     */
    public void banPlayer(int uid, long endTime, String reason, boolean isBanIp, String bannedBy) {
        Player player = getGameContext().getPlayerModule().getPlayer(uid);

        // It is only used as a supplement to find the banned IP when unblocking a player
        String playerBindIp = null;

        if (isBanIp) {
            if (player != null && player.getSession() != null) {
                String ipAddress = player.getSession().getIpAddress();
                if (ipAddress != null && !ipAddress.isEmpty()) {
                    playerBindIp = ipAddress;
                    banIp(ipAddress, endTime, reason, bannedBy);
                }
            }
        }

        BanInfo banInfo = new BanInfo(uid, endTime, reason, bannedBy, playerBindIp);
        cachedPlayerBans.put(uid, banInfo);
        banInfo.save();

        // Kick player
        if (player != null && player.isLoaded()) {
            player.setSession(null);
        }
    }

    /**
     * Ban an IP address
     * <p>
     * Please be cautious about disabling IPs
     * in some regions where IPs are scarce, many people may share a public IP
     * and restarting the optical cat device will reassign a new IP
     *
     * @param ipAddress IP address to ban
     * @param endTime Ban expiration time (0 = permanent)
     * @param reason Ban reason
     * @param bannedBy Who banned the IP
     */
    public void banIp(String ipAddress, long endTime, String reason, String bannedBy) {
        BanInfo banInfo = new BanInfo(0, endTime, reason, bannedBy, ipAddress);
        cachedIpBans.put(ipAddress, banInfo);
        banInfo.save();

        List<Player> playerList = Nebula.getGameContext().getPlayerModule().getCachedPlayers()
                .values().stream().toList();

        String playerIpAddress;
        for (Player player : playerList) {
            playerIpAddress = player.getSession().getIpAddress();
            if (playerIpAddress == null) {
                return;
            }

            // Kick player
            if (playerIpAddress.equals(ipAddress)) {
                player.setSession(null);
            }
        }
    }

    /**
     * Unban a player
     * @param uid Player UID
     */
    public void unbanPlayer(int uid) {
        BanInfo banInfo = cachedPlayerBans.remove(uid);
        if (banInfo == null) {
            banInfo = getPlayerBanInfo(uid);
        }

        if (banInfo == null) {
            return;
        }

        deleteBan(banInfo);
    }
    
    /**
     * Unban an IP address
     * @param ipAddress IP address to unban
     */
    public void unbanIp(String ipAddress) {
        BanInfo banInfo = cachedIpBans.remove(ipAddress);
        if (banInfo == null) {
            banInfo = getIpBanInfo(ipAddress);
        }

        if (banInfo == null) {
            return;
        }

        deleteBan(banInfo);
    }
    
    /**
     * Check if a player is banned
     * @param uid Player UID
     * @return True if banned, false otherwise
     */
    public boolean isPlayerBanned(int uid) {
        BanInfo banInfo = cachedPlayerBans.get(uid);
        if (banInfo == null) {
            banInfo = loadPlayerBanFromDatabase(uid);
            if (banInfo == null) {
                return false;
            }
            cachedPlayerBans.put(uid, banInfo);
        }
        
        // Check if ban has expired
        if (banInfo.isExpired()) {
            unbanPlayer(uid);
            return false;
        }
        
        return true;
    }
    
    /**
     * Check if an IP address is banned
     * @param ipAddress IP address
     * @return True if banned, false otherwise
     */
    public boolean isIpBanned(String ipAddress) {
        BanInfo banInfo = cachedIpBans.get(ipAddress);
        if (banInfo == null) {
            banInfo = loadIpBanFromDatabase(ipAddress);
            if (banInfo == null) {
                return false;
            }
            cachedIpBans.put(ipAddress, banInfo);
        }
        
        // Check if ban has expired
        if (banInfo.isExpired()) {
            unbanIp(ipAddress);
            return false;
        }
        
        return true;
    }
    
    /**
     * Get ban info for a player
     * @param uid Player UID
     * @return BanInfo or null if not banned
     */
    public BanInfo getPlayerBanInfo(int uid) {
        BanInfo banInfo = cachedPlayerBans.get(uid);
        if (banInfo == null) {
            banInfo = loadPlayerBanFromDatabase(uid);
            if (banInfo != null) {
                cachedPlayerBans.put(uid, banInfo);
            }
        }
        return banInfo;
    }
    
    /**
     * Get ban info for an IP
     * @param ipAddress IP address
     * @return BanInfo or null if not banned
     */
    public BanInfo getIpBanInfo(String ipAddress) {
        BanInfo banInfo = cachedIpBans.get(ipAddress);
        if (banInfo == null) {
            banInfo = loadIpBanFromDatabase(ipAddress);
            if (banInfo != null) {
                cachedIpBans.put(ipAddress, banInfo);
            }
        }
        return banInfo;
    }
    
    /**
     * Delete ban from database
     */
    private void deleteBan(BanInfo banInfo) {
        try {
            Nebula.getGameDatabase().delete(banInfo);
        } catch (Exception e) {
            Nebula.getLogger().error("Failed to delete ban info from database", e);
        }
    }
    
    /**
     * Load player ban from database
     */
    private BanInfo loadPlayerBanFromDatabase(int uid) {
        if (uid == 0)
            return null;

        try {
            return Nebula.getGameDatabase().getObjectByField(BanInfo.class, "playerUid", uid);
        } catch (Exception e) {
            Nebula.getLogger().error("Failed to load player ban from database", e);
        }
        return null;
    }
    
    /**
     * Load IP ban from database
     */
    private BanInfo loadIpBanFromDatabase(String ipAddress) {
        if (ipAddress == null || ipAddress.isEmpty())
            return null;

        try {
            return Nebula.getGameDatabase().getObjectByField(BanInfo.class, "ipAddress", ipAddress);
        } catch (Exception e) {
            Nebula.getLogger().error("Failed to load IP ban from database", e);
        }
        return null;
    }
}