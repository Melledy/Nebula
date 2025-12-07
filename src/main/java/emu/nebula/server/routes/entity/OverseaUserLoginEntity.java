package emu.nebula.server.routes.entity;

import java.util.List;

public class OverseaUserLoginEntity {
    public int Code;
    public UserDetailJson Data;
    public String Msg;
    
    public static class UserDetailJson {
        public long AgeVerifyMethod;
        public Object Destroy;
        public boolean IsTestAccount;
        public List<UserKeyJson> Keys;
        public long ServerNowAt;
        public UserInfoJson UserInfo;
        public LoginYostarJson Yostar;
        public Object YostarDestroy;
    }
    
    public static class UserKeyJson {
        public String ID;
        public String Type;
        public String Key;
        public String NickName;
        public long CreatedAt;
    }
    
    public static class UserInfoJson {
        public String ID;
        public int UID2;
        public String PID;
        public String Token;
        public String Birthday;
        public String RegChannel;
        public String TransCode;
        public int State;
        public String DeviceID;
        public long CreatedAt;
    }

    public static class LoginYostarJson {
        public String ID;
        public String Country;
        public String Nickname;
        public String Picture;
        public int State;
        public int AgreeAd;
        public long CreatedAt;
    }
}
