package emu.nebula.command.commands;

import emu.nebula.command.Command;
import emu.nebula.command.CommandArgs;
import emu.nebula.command.CommandHandler;
import emu.nebula.game.account.AccountHelper;
import emu.nebula.util.Utils;

@Command(label = "account", permission = "admin.account", desc = "!account {create | delete} [email] (reserved player uid). Creates or deletes an account.")
public class AccountCommand implements CommandHandler {

    @Override
    public String execute(CommandArgs args) {
        if (args.size() < 1) {
            return "Invalid amount of args";
        }
        
        String command = args.get(0).toLowerCase();
        

        switch (command) {
            case "create" -> {
                // Get username
                if (args.size() < 2) {
                    return "Invalid amount of args";
                }
                
                String username = args.get(1);
                
                // Reserved player uid
                int reservedUid = 0;
                
                if (args.size() >= 3) {
                    reservedUid = Utils.parseSafeInt(args.get(2));
                }
    
                if (AccountHelper.createAccount(username, null, reservedUid) != null) {
                    return "Account created";
                } else {
                    return "Account already exists";
                }
            }
            case "delete" -> {
                // Get username
                if (args.size() < 2) {
                    return "Invalid amount of args";
                }
                
                String username = args.get(1);
                
                // Delete account
                if (AccountHelper.deleteAccount(username)) {
                    return "Account deleted";
                } else {
                    return "Account doesnt exist";
                }
            }
            case "list" -> {
                var list = AccountHelper.getAccountList();
                var sb = new StringBuilder();
                
                sb.append("Listing all accounts:");
                sb.append("\n");
                
                for (var account : list) {
                    sb.append(account.getUid() + " : " + account.getEmail());
                    sb.append("\n");
                }
                
                return sb.toString();
            }
        }
        
        // Fallback
        return "Account sub command not found";
    }

}
