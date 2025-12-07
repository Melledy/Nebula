package emu.nebula.server.routes.entity;

import java.util.List;

public class ChinaUserLoginEntity {
    public int Code;
    public UserDetailJson Data;
    public String Msg;
    
    public static class UserDetailJson {
        public Object Destroy;
        public Object YostarDestroy;

        public boolean IsNew;
        public boolean IsTestAccount;
        public List<UserKeyJson> Keys;
        public UserJson User;
        public LoginYostarJson Yostar;
        public Object TaptapProfile;
        public IdentityJson Identity;
    }

    public static class UserJson {
        public long ID;
        public String PID;
        public String Token;
        public int State;
        public String RegChannel;
        public int DestroyState;
    }
    
    public static class UserKeyJson {
        public String Type;
        public String NickName;
        public String NickNameEnc;
    }

    public static class LoginYostarJson {
        public long ID;
        public String NickName;
        public String Picture;
        public int State;
        public long CreatedAt;
        public String DefaultNickName;
    }

    public static class IdentityJson {
        public int Type;
        public String RealName;
        public String IDCard;
        public boolean Underage;
        public String PI;
        public String BirthDate;
        public int State;
    }
}
