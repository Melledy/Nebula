package emu.nebula.command.commands;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import emu.nebula.command.Command;
import emu.nebula.command.CommandArgs;
import emu.nebula.command.CommandHandler;
import emu.nebula.game.mail.GameMail;
import emu.nebula.util.Utils;

@Command(
    label = "mail",
    aliases = {"m"},
    permission = "player.mail",
    requireTarget = true,
    desc = "!mail \"subject\" \"body\" [itemId xQty | itemId:qty ...] = Sends the targeted player a system mail. The subject and body must be wrapped in quotes, and attachments are optional."
)
public class MailCommand implements CommandHandler {
    private static final String USAGE_TEXT = "Usage: /mail \"subject\" \"body\" [itemId xQty | itemId:qty ...]";
    private static final Pattern QUOTED_TEXT = Pattern.compile("\"([^\"]*)\"");

    @Override
    public String execute(CommandArgs args) {
        var target = args.getTarget();
        if (target == null) {
            return "Error - Targeted player not found or offline";
        }

        String rawInput = args.getRaw() == null ? "" : args.getRaw().trim();
        if (rawInput.isEmpty()) {
            return USAGE_TEXT;
        }

        Matcher matcher = QUOTED_TEXT.matcher(rawInput);
        if (!matcher.find()) {
            return USAGE_TEXT;
        }

        String subject = matcher.group(1).trim();
        if (!matcher.find()) {
            return "Mail body must be wrapped in quotes after the subject.";
        }

        String body = matcher.group(1).trim();
        int attachmentStartIndex = matcher.end();

        if (subject.isEmpty()) {
            return "Mail subject cannot be empty.";
        }

        if (body.isEmpty()) {
            body = " "; // just for safety, so the client won't complain
        }

        String author = args.getSender() != null ? args.getSender().getName() : "System";
        var mail = new GameMail(author, subject, body);

        String attachmentSection = rawInput.length() > attachmentStartIndex
                ? rawInput.substring(attachmentStartIndex).trim()
                : "";

        parseAttachments(attachmentSection, mail, args);
        
        target.getMailbox().sendMail(mail);
        return "Mail sent to " + target.getName() + " with subject \"" + subject + "\".";
    }

    private void parseAttachments(String attachmentSection, GameMail mail, CommandArgs args) {
        if (attachmentSection == null || attachmentSection.isBlank()) {
            return;
        }

        Integer pendingItemId = null;
        for (String token : attachmentSection.split("\\s+")) {
            if (token.isBlank()) {
                continue;
            }

            if (token.contains(":") || token.contains(",")) {
                String[] split = token.split("[:,]", 2);
                int itemId = Utils.parseSafeInt(split[0]);
                int qty = split.length > 1 ? Utils.parseSafeInt(split[1]) : 1;
                addAttachment(mail, args, itemId, qty);
                pendingItemId = null;
                continue;
            }

            if (token.startsWith("x") && token.length() > 1) {
                if (pendingItemId == null) {
                    //args.sendMessage("Quantity token '" + token + "' must follow an item id.");
                    continue;
                }

                int qty = Utils.parseSafeInt(token.substring(1));
                addAttachment(mail, args, pendingItemId, qty);
                pendingItemId = null;
                continue;
            }

            if (isIntegerToken(token)) {
                if (pendingItemId != null) {
                    addAttachment(mail, args, pendingItemId, 1);
                }

                int itemId = Utils.parseSafeInt(token);
                if (itemId <= 0) {
                    //args.sendMessage("Invalid item id '" + token + "'.");
                    pendingItemId = null;
                    continue;
                }

                pendingItemId = itemId;
                continue;
            }

            //args.sendMessage("Ignoring attachment token '" + token + "'.");
        }

        if (pendingItemId != null) {
            addAttachment(mail, args, pendingItemId, 1);
        }
    }

    private void addAttachment(GameMail mail, CommandArgs args, int itemId, int quantity) {
        if (itemId <= 0) {
            //args.sendMessage("Item id must be positive.");
            return;
        }

        int qty = Math.max(quantity, 1);
        mail.addAttachment(itemId, qty);
    }

    private boolean isIntegerToken(String token) {
        for (int i = 0; i < token.length(); i++) {
            if (!Character.isDigit(token.charAt(i))) {
                return false;
            }
        }
        return !token.isEmpty();
    }
}
