package emu.nebula.server.routes;

import java.util.ArrayList;

import emu.nebula.Nebula;
import emu.nebula.server.routes.entity.ChinaUserLoginEntity;
import emu.nebula.server.routes.entity.OverseaUserLoginEntity;
import org.jetbrains.annotations.NotNull;

import emu.nebula.game.account.Account;
import emu.nebula.game.account.AccountHelper;
import emu.nebula.util.JsonUtils;
import io.javalin.http.ContentType;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import lombok.Getter;

@Getter
public class UserLoginHandler implements Handler {

    @Override
    public void handle(@NotNull Context ctx) throws Exception {
        // Get account from header first
        Account account = this.getAccountFromHeader(ctx);
        
        // Check req body for account details
        if (account == null) {
            account = this.getAccountFromBody(ctx);
        }
        
        // Result
        ctx.contentType(ContentType.APPLICATION_JSON);
        ctx.result(getJsonResult(ctx, account));
    }

    private String getJsonResult(Context ctx, Account account) {
        if (account == null) {
            return "{\"Code\":100403,\"Data\":{},\"Msg\":\"Error\"}";
        }

        String Channel = "";
        var req = JsonUtils.decode(ctx.header("Authorization"), UserAuthDataJson.class);

        if (req != null) {
            Channel = req.Head.Channel;
        }

        String token = account.getLoginToken();
        if (token == null || token.isEmpty()) {
            account.generateLoginToken();
        }

        String nickName = account.getNickname();
        if (nickName == null || nickName.isEmpty()) {
            account.setNickname(account.getEmail());
        }

        // Check if it's a CN channel
        // The data required by CN channels is somewhat different from other regional servers
        if (Channel != null && Channel.equals("official")) {
            // CN
            var response = new ChinaUserLoginEntity();

            response.Code = 200;
            response.Msg = "OK";
            response.Data = new ChinaUserLoginEntity.UserDetailJson();
            response.Data.IsNew = false;
            response.Data.IsTestAccount = false;
            response.Data.Keys = new ArrayList<>();
            response.Data.User = new ChinaUserLoginEntity.UserJson();
            response.Data.Yostar = new ChinaUserLoginEntity.LoginYostarJson();
            response.Data.Identity = new ChinaUserLoginEntity.IdentityJson();
            response.Data.TaptapProfile = null;

            response.Data.User.ID = Long.parseLong(account.getUid());
            response.Data.User.PID = "NEBULA";
            response.Data.User.Token = account.getLoginToken();
            response.Data.User.State = 1;
            response.Data.User.RegChannel = "pc_official";
            response.Data.User.DestroyState = 0;

            response.Data.Yostar.ID = Long.parseLong(account.getUid());
            response.Data.Yostar.NickName = account.getNickname();
            response.Data.Yostar.Picture = account.getPicture();
            response.Data.Yostar.State = 1;
            response.Data.Yostar.CreatedAt = account.getCreatedAt();
            response.Data.Yostar.DefaultNickName = "";

            response.Data.Identity.Type = 0;
            response.Data.Identity.RealName = "***";
            response.Data.Identity.IDCard = "******************";
            response.Data.Identity.Underage = false;
            response.Data.Identity.PI = "";
            response.Data.Identity.BirthDate = "";
            response.Data.Identity.State = 1;

            var key = new ChinaUserLoginEntity.UserKeyJson();
            key.Type = "mobile";
            key.NickName = account.getEmail();
            key.NickNameEnc = "";

            response.Data.Keys.add(key);
            return JsonUtils.encode(response, true);
        } else {
            // OS
            var response = new OverseaUserLoginEntity();

            response.Code = 200;
            response.Msg = "OK";
            response.Data = new OverseaUserLoginEntity.UserDetailJson();
            response.Data.Keys = new ArrayList<>();
            response.Data.UserInfo = new OverseaUserLoginEntity.UserInfoJson();
            response.Data.Yostar = new OverseaUserLoginEntity.LoginYostarJson();

            response.Data.UserInfo.ID = account.getUid();
            response.Data.UserInfo.UID2 = 0;
            response.Data.UserInfo.PID = "NEBULA";
            response.Data.UserInfo.Token = account.getLoginToken();
            response.Data.UserInfo.Birthday = "";
            response.Data.UserInfo.RegChannel = "pc";
            response.Data.UserInfo.TransCode = "";
            response.Data.UserInfo.State = 1;
            response.Data.UserInfo.DeviceID = "";
            response.Data.UserInfo.CreatedAt = account.getCreatedAt();

            response.Data.Yostar.ID = account.getUid();
            response.Data.Yostar.Country = "US";
            response.Data.Yostar.Nickname = account.getNickname();
            response.Data.Yostar.Picture = account.getPicture();
            response.Data.Yostar.State = 1;
            response.Data.Yostar.AgreeAd = 0;
            response.Data.Yostar.CreatedAt = account.getCreatedAt();

            var key = new OverseaUserLoginEntity.UserKeyJson();
            key.ID = account.getUid();
            key.Type = "yostar";
            key.Key = account.getEmail();
            key.NickName = account.getEmail();
            key.CreatedAt = account.getCreatedAt();

            response.Data.Keys.add(key);

            return JsonUtils.encode(response, true);
        }
    }
    
    protected Account getAccountFromBody(Context ctx) {
        // Parse request
        var req = JsonUtils.decode(ctx.body(), UserLoginRequestJson.class);
        
        if (req == null || req.OpenID == null || req.Token == null) {
            return null;
        }

        Account account = AccountHelper.getAccountByLoginToken(req.Token);

        if (account != null) return account;

        // In CN region, login method is mobile phone + MFA verification code
        // OpenID is mobile phone number, Token is a six-digit verification code
        // These are stored in the body
        {
            // CN region requires an OpenID query
            account = AccountHelper.getAccountByEmail(req.OpenID);
            if (account == null) {
                // Create an account if were allowed to
                if (Nebula.getConfig().getServerOptions().isAutoCreateAccount()) {
                    account = AccountHelper.createAccount(req.OpenID, null, 0);
                }
            } else {
                // Check passcode sent by email
                if (!account.verifyCode(req.Token)) {
                    account = null;
                }
            }
        }
        
        // Get account
        return account;
    }
    
    protected Account getAccountFromHeader(Context ctx) {
        // Parse request
        var req = JsonUtils.decode(ctx.header("Authorization"), UserAuthDataJson.class);
        
        if (req == null || req.Head == null || req.Head.Token == null) {
            return null;
        }
        
        // Get account
        return AccountHelper.getAccountByLoginToken(req.Head.Token); 
    }
    
    @SuppressWarnings("unused")
    private static class UserLoginRequestJson {
        public String OpenID;
        public String Token;
        public String Type;
        public String UserName;
        public String Secret;
        public int CheckAccount;
    }
    
    @SuppressWarnings("unused")
    private static class UserAuthDataJson {
        public UserAuthHeadJson Head;
        public String Sign;
        
        protected static class UserAuthHeadJson {
            public String UID;
            public String Token;
            public String Channel;
        }
    }
    
}
