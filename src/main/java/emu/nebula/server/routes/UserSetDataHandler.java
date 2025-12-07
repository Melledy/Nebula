package emu.nebula.server.routes;

import org.jetbrains.annotations.NotNull;

import emu.nebula.game.account.Account;
import emu.nebula.util.JsonUtils;
import io.javalin.http.ContentType;
import io.javalin.http.Context;
import lombok.Getter;

@Getter
public class UserSetDataHandler extends UserLoginHandler {

    @Override
    public void handle(@NotNull Context ctx) throws Exception {
        // Get account from header first
        Account account = this.getAccountFromHeader(ctx);
        
        // Check
        if (account == null) {
            ctx.contentType(ContentType.APPLICATION_JSON);
            ctx.result("{\"Code\":100403,\"Data\":{},\"Msg\":\"Error\"}"); // TOKEN_AUTH_FAILED
            return;
        }
        
        // Parse request
        var req = JsonUtils.decode(ctx.body(), UserSetDataReqJson.class);
        
        if (req.Key == null || req.Value == null) {
            ctx.contentType(ContentType.APPLICATION_JSON);
            ctx.result("{\"Code\":100110,\"Data\":{},\"Msg\":\"Error\"}"); // VALID_FAIL
            return;
        }

        // OS uses the former, CN uses the latter
        if (req.Key.equals("Nickname") || req.Key.equals("nickname")) {
            account.setNickname(req.Value);
            account.save();
        }
        
        // Result
        ctx.contentType(ContentType.APPLICATION_JSON);
        ctx.result("{\"Code\":200,\"Data\":{},\"Msg\":\"OK\"}");
    }
    
    private static class UserSetDataReqJson {
        public String Key;
        public String Value;
    }
    
}
