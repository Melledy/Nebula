package emu.nebula.game.account;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;

import emu.nebula.Nebula;
import emu.nebula.game.player.Player;

/**
 * Helper class for handling account related stuff
 */
public class AccountHelper {

    public static Account createAccount(String email, String password, int reservedUid) {
        Account account = Nebula.getAccountDatabase().getObjectByField(Account.class, "email", email);
        
        if (account != null) {
            return null;
        }
        
        account = new Account(email, password, reservedUid);
        account.save();
        
        return account;
    }
    
    public static Account getAccountByEmail(String email) {
        if (email == null || email.isEmpty()) {
            return null;
        }
        
        return Nebula.getAccountDatabase().getObjectByField(Account.class, "email", email);
    }
    
    public static Account getAccountByLoginToken(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        
        return Nebula.getAccountDatabase().getObjectByField(Account.class, "loginToken", token);
    }
    
    public static List<Account> getAccountList() {
        return Nebula.getAccountDatabase().getObjects(Account.class).toList();
    }
    
    public static boolean deleteAccount(String email) {
        // Get account
        Account account = Nebula.getAccountDatabase().getObjectByField(Account.class, "email", email);

        if (account == null) {
            return false;
        }
        
        // Delete player
        if (Nebula.getGameContext() != null) {
            var player = Nebula.getGameDatabase().getObjectByField(Player.class, "accountUid", account.getUid());
            if (player != null) {
                Nebula.getGameContext().getPlayerModule().deletePlayer(player.getUid());
            }
        }
        
        // Delete the account
        return Nebula.getAccountDatabase().delete(account);
    }
    
    // Simple way to create a unique session key
    public static String createSessionKey(String accountUid) {
        byte[] random = new byte[64];
        
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(random);
        
        String temp = accountUid + "." + System.currentTimeMillis() + "." + secureRandom.toString();
        
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            byte[] bytes = md.digest(temp.getBytes());
            return Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            return Base64.getEncoder().encodeToString(temp.getBytes());
        }
    }
}
