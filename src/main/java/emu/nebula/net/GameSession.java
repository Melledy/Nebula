package emu.nebula.net;

import java.security.MessageDigest;
import java.util.Base64;

import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;

import emu.nebula.Nebula;
import emu.nebula.game.account.Account;
import emu.nebula.game.account.AccountHelper;
import emu.nebula.game.player.Player;
import emu.nebula.proto.Public.MailState;
import emu.nebula.proto.Public.Nil;
import emu.nebula.util.AeadHelper;
import emu.nebula.util.Utils;
import lombok.Getter;
import lombok.SneakyThrows;
import us.hebi.quickbuf.ProtoMessage;
import us.hebi.quickbuf.RepeatedByte;

@Getter
public class GameSession {
    private String token;
    private Account account;
    private Player player;

    // Crypto
    private int encryptMethod; // 0 = gcm, 1 = chacha20
    private byte[] clientPublicKey;
    private byte[] serverPublicKey;
    private byte[] serverPrivateKey;
    private byte[] key;

    // Session cleanup
    private boolean remove;
    private long lastActiveTime;

    public GameSession() {
        this.updateLastActiveTime();
    }

    public synchronized Player getPlayer() {
        return this.player;
    }

    public synchronized void setPlayer(Player player) {
        this.player = player;
        this.player.setSession(this);
        this.player.onLogin();
    }

    public synchronized void clearPlayer() {
        // Sanity check
        if (this.player == null) {
            return;
        }

        // Clear player
        var player = this.player;
        this.player = null;

        // Clear remote token
        player.setPlayerRemoteToken(null);
        player.save();

        // Remove session from player
        player.removeSession();

        // Set remove flag
        this.remove = true;
    }

    public synchronized boolean hasPlayer() {
        return this.player != null;
    }

    // Encryption

    public void setClientKey(RepeatedByte key) {
        this.clientPublicKey = key.toArray();
    }

    public void generateServerKey() {
        var pair = AeadHelper.generateECDHKEyPair();

        this.serverPrivateKey = ((ECPrivateKeyParameters) pair.getPrivate()).getD().toByteArray();
        this.serverPublicKey = ((ECPublicKeyParameters) pair.getPublic()).getQ().getEncoded(false);
    }

    public void calculateKey() {
        this.key = AeadHelper.generateKey(clientPublicKey, serverPublicKey, serverPrivateKey);
        this.encryptMethod = Utils.randomRange(0, 1);
    }

    public String generateToken() {
        String temp = System.currentTimeMillis() + ":" + AeadHelper.generateBytes(64).toString();

        try {
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            byte[] bytes = md.digest(temp.getBytes());

            this.token = Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            this.token = Base64.getEncoder().encodeToString(temp.getBytes());
        }

        return this.token;
    }

    // Login

    public boolean login(String loginToken) {
        // Sanity check
        if (this.account != null) {
            return false;
        }

        // Get account
        this.account = AccountHelper.getAccountByLoginToken(loginToken);

        if (account == null) {
            return false;
        }

        // Note: We should cache players in case multiple sessions try to login to the
        // same player at the time
        // Get player by account
        var player = Nebula.getGameContext().getPlayerModule().loadPlayer(account);

        // Skip intro
        if (player == null && Nebula.getConfig().getServerOptions().skipIntro) {
            player = Nebula.getGameContext().getPlayerModule().createPlayer(this, "Player", false);
        }

        // Set player
        if (player != null) {
            this.setPlayer(player);
        }

        return true;
    }

    public void updateLastActiveTime() {
        this.lastActiveTime = System.currentTimeMillis();
    }

    // Packet encoding helper functions

    @SneakyThrows
    public byte[] encodeMsg(int msgId, ProtoMessage<?> proto) {
        // Check if we have any packages to send to the client
        if (this.getPlayer() != null) {
            // Check if player should add any packages
            this.checkPlayerStates();

            // Chain next packages for player
            if (this.getPlayer().hasNextPackages()) {
                this.addNextPackages(proto);
            }
        }

        // Encode to message like normal
        return PacketHelper.encodeMsg(msgId, proto);
    }

    public byte[] encodeMsg(int msgId) {
        // Check if we have any packages to send to the client
        if (this.getPlayer() != null) {
            // Check if player should add any packages
            this.checkPlayerStates();

            // Chain next packages for player
            if (this.getPlayer().hasNextPackages()) {
                // Create a proto so we can add next packages
                var proto = Nil.newInstance();

                // Encode proto with next packages
                return this.encodeMsg(msgId, this.addNextPackages(proto));
            }
        }

        // Encode simple message
        return PacketHelper.encodeMsg(msgId);
    }

    private void checkPlayerStates() {
        // Update mail state flag
        if (this.getPlayer().getMailbox().isNewState()) {
            // Clear
            this.getPlayer().getMailbox().clearNewState();

            // Send mail state notify
            this.getPlayer().addNextPackage(
                    NetMsgId.mail_state_notify,
                    MailState.newInstance().setNew(true));
        }

        // Check handbook states
        if (this.getPlayer().getCharacters().isUpdateCharHandbook()) {
            getPlayer().getCharacters().setUpdateCharHandbook(false);
            getPlayer().addNextPackage(
                    NetMsgId.handbook_change_notify,
                    this.getPlayer().getCharacters().getCharacterHandbook());
        }
        if (this.getPlayer().getCharacters().isUpdateDiscHandbook()) {
            getPlayer().getCharacters().setUpdateDiscHandbook(false);
            getPlayer().addNextPackage(
                    NetMsgId.handbook_change_notify,
                    this.getPlayer().getCharacters().getDiscHandbook());
        }
    }

    private ProtoMessage<?> addNextPackages(ProtoMessage<?> proto) {
        // Sanity check and make sure proto has a "nextPackage" field
        if (!PacketHelper.hasNextPackageMethod(proto)) {
            return proto;
        }

        // Set next package
        if (this.getPlayer().getNextPackages().size() > 0) {
            // Set current package
            NetMsgPacket curPacket = null;

            // Chain link next packages
            while (getPlayer().getNextPackages().size() > 0) {
                // Make sure the current packet has a nextPackage field
                if (curPacket != null && !PacketHelper.hasNextPackageMethod(curPacket.getProto())) {
                    break;
                }

                // Get current package
                var nextPacket = getPlayer().getNextPackages().pop();

                // Set cur packet if its null
                if (curPacket == null) {
                    curPacket = nextPacket;
                    continue;
                }

                // Set next package
                PacketHelper.setNextPackage(nextPacket.getProto(), curPacket.toByteArray());

                // Update next packet
                curPacket = nextPacket;
            }

            // Set next package of current proto via reflection
            if (curPacket != null) {
                PacketHelper.setNextPackage(proto, curPacket.toByteArray());
            }
        }

        return proto;
    }
}
