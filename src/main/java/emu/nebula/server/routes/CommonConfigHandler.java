package emu.nebula.server.routes;

import emu.nebula.util.JsonUtils;
import org.jetbrains.annotations.NotNull;

import emu.nebula.server.HttpServer;
import io.javalin.http.ContentType;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import lombok.Getter;

@Getter
public class CommonConfigHandler implements Handler {
    private String osConfig = "{\"Code\":200,\"Data\":{\"AppConfig\":{\"ACCOUNT_RETRIEVAL\":{\"FIRST_LOGIN_POPUP\":false,\"LOGIN_POPUP\":false,\"PAGE_URL\":\"\"},\"AGREEMENT_POPUP_TYPE\":\"Browser\",\"APPLE_CURRENCY_BLOCK_LIST\":null,\"APPLE_TYPE_KEY\":\"apple_hk\",\"APP_CLIENT_LANG\":[\"en\"],\"APP_DEBUG\":0,\"APP_GL\":\"en\",\"BIND_METHOD\":[\"google\",\"apple_hk\",\"facebook\"],\"CAPTCHA_ENABLED\":false,\"CLIENT_LOG_REPORTING\":{\"ENABLE\":false},\"CREDIT_INVESTIGATION\":\"0.0\",\"DESTROY_USER_DAYS\":15,\"DESTROY_USER_ENABLE\":1,\"DETECTION_ADDRESS\":{\"AUTO\":{\"DNS\":[\"${url}\",\"${url}\",\"${url}/meta/serverlist.html\"],\"HTTP\":[\"${url}\",\"${url}\",\"${url}\"],\"MTR\":[\"${url}\",\"${url}\",\"${url}/meta/serverlist.html\"],\"PING\":[\"${url}\",\"${url}\",\"${url}/meta/serverlist.html\"],\"TCP\":[\"${url}\",\"${url}\",\"${url}/meta/serverlist.html\"]},\"ENABLE\":true,\"ENABLE_MANUAL\":true,\"INTERNET\":\"https://www.google.com\",\"INTERNET_ADDRESS\":\"https://www.google.com\",\"NETWORK_ENDPORINT\":\"\",\"NETWORK_PROJECT\":\"\",\"NETWORK_SECRET_KEY\":\"\"},\"ENABLE_AGREEMENT\":true,\"ENABLE_MULTI_LANG_AGREEMENT\":false,\"ENABLE_TEXT_REVIEW\":true,\"ERROR_CODE\":\"4.4\",\"FILE_DOMAIN\":\"\",\"GEETEST_ENABLE\":false,\"GEETEST_ID\":\"\",\"GOOGLE_ANALYTICS_MEASUREMENT_ID\":\"\",\"MIGRATE_POPUP\":true,\"NICKNAME_REG\":\"^[A-Za-z0-9]{2,20}$\",\"POPUP\":{\"Data\":[{\"Lang\":\"ja\",\"Text\":\"YostarIDを作成\"},{\"Lang\":\"en\",\"Text\":\"CreateaYostaraccount\"},{\"Lang\":\"kr\",\"Text\":\"YOSTAR계정가입하기\"},{\"Lang\":\"fr\",\"Text\":\"CréezvotrecompteYostar\"},{\"Lang\":\"de\",\"Text\":\"EinenYostar-Accounterstellen\"}],\"Enable\":true},\"PRIVACY_AGREEMENT\":\"0.1\",\"RECHARGE_LIMIT\":{\"Enable\":false,\"IsOneLimit\":false,\"Items\":[],\"OneLimitAmount\":0},\"SHARE\":{\"CaptureScreen\":{\"AutoCloseDelay\":0,\"Enabled\":false},\"Facebook\":{\"AppID\":\"\",\"Enabled\":false},\"Instagram\":{\"Enabled\":false},\"Kakao\":{\"AppKey\":\"\",\"Enabled\":false},\"Naver\":{\"Enabled\":false},\"Twitter\":{\"Enabled\":false}},\"SLS\":{\"ACCESS_KEY_ID\":\"7b5d0ffd0943f26704fc547a871c68b1b5d56b5c9caeb354205b81f445d7af59\",\"ACCESS_KEY_SECRET\":\"4a5e9cc8a50819290c9bfa1fedc79da7c50e85189a05eb462a3d28a7688eabb0\",\"ENABLE\":false},\"SURVEY_POPUP_TYPE\":\"Browser\",\"UDATA\":{\"Enable\":false,\"URL\":\"${url}\"},\"USER_AGREEMENT\":\"0.1\",\"YOSTAR_PREFIX\":\"yoyo\"},\"EuropeUnion\":false,\"StoreConfig\":{\"ADJUST_APPID\":\"\",\"ADJUST_CHARGEEVENTTOKEN\":\"\",\"ADJUST_ENABLED\":0,\"ADJUST_EVENTTOKENS\":null,\"ADJUST_ISDEBUG\":0,\"AIRWALLEX_ENABLED\":false,\"AI_HELP\":{\"AihelpAppID\":\"yostar1_platform_2db52a57068b1ee3fe3652c8b53d581b\",\"AihelpAppKey\":\"YOSTAR1_app_bc226f4419a7447c9de95711f8a2d3d9\",\"AihelpDomain\":\"yostar1.aihelp.net\",\"CustomerServiceURL\":\"\",\"CustomerWay\":1,\"DisplayType\":\"Browser\",\"Enable\":1,\"Mode\":\"robot\"},\"APPLEID\":\"\",\"CODA_ENABLED\":false,\"ENABLED_PAY\":{\"AIRWALLEX_ENABLED\":false,\"CODA_ENABLED\":false,\"GMOAlipay\":false,\"GMOAu\":false,\"GMOCreditcard\":false,\"GMOCvs\":false,\"GMODocomo\":false,\"GMOPaypal\":false,\"GMOPaypay\":false,\"GMOSoftbank\":false,\"MYCARD_ENABLED\":false,\"PAYPAL_ENABLED\":true,\"RAZER_ENABLED\":false,\"STEAM_ENABLED\":false,\"STRIPE_ENABLED\":true,\"TOSS_ENABLED\":false,\"WEBMONEY_ENABLED\":false},\"FACEBOOK_APPID\":\"\",\"FACEBOOK_CLIENT_TOKEN\":\"\",\"FACEBOOK_SECRET\":\"\",\"FIREBASE_ENABLED\":0,\"GMO_CC_JS\":\"https://\",\"GMO_CC_KEY\":\"\",\"GMO_CC_SHOPID\":\"\",\"GMO_PAY_CHANNEL\":{\"GMOAlipay\":false,\"GMOAu\":false,\"GMOCreditcard\":false,\"GMOCvs\":false,\"GMODocomo\":false,\"GMOPaypal\":false,\"GMOPaypay\":false,\"GMOSoftbank\":false},\"GMO_PAY_ENABLED\":false,\"GOOGLE_CLIENT_ID\":\"\",\"GOOGLE_CLIENT_SECRET\":\"\",\"GUEST_CREATE_METHOD\":0,\"GUIDE_POPUP\":{\"DATA\":null,\"ENABLE\":0},\"LOGIN\":{\"DEFAULT\":\"yostar\",\"ICON_SIZE\":\"big\",\"SORT\":[\"google\",\"apple\",\"device\"]},\"MYCARD_ENABLED\":false,\"ONE_STORE_LICENSE_KEY\":\"\",\"PAYPAL_ENABLED\":false,\"RAZER_ENABLED\":false,\"REMOTE_CONFIG\":[],\"SAMSUNG_SANDBOX_MODE\":false,\"STEAM_APPID\":\"\",\"STEAM_ENABLED\":false,\"STEAM_PAY_APPID\":\"\",\"STRIPE_ENABLED\":false,\"TOSS_ENABLED\":false,\"TWITTER_KEY\":\"\",\"TWITTER_SECRET\":\"\",\"WEBMONEY_ENABLED\":false}},\"Msg\":\"OK\"}";
    private String cnConfig = "{\"Code\":200,\"Data\":{\"AppConfig\":{\"AppropriateAge\":{\"Level\":\"18+\",\"Msg\":\"《星塔旅人》游戏适龄提示\\n1、本游戏是一款玩法简单的角色扮演游戏，适用于年满18周岁及以上的用户。\\n2、本游戏基于架空的故事背景和幻想世界观，剧情简单且积极向上，没有基于真实历史和现实事件的改编内容。游戏玩法基于肢体操作，鼓励玩家通过训练达成目标。游戏中有少量自定义文字系统，该社交系统遵循相关法律法规进行管理。\\n3、本游戏中有用户实名认证系统，对年满18周岁及以上的用户开放，使用18周岁以下的身份信息认证账号将无法进入游戏。\"},\"Captcha\":{\"AppID\":191947906,\"Enable\":true},\"DestroyUser\":{\"Days\":15,\"Enable\":true},\"DetectionAddress\":{\"Auto\":{\"DNS\":[\"${url}/meta/serverlist.html\",\"${url}\"],\"HTTP\":[\"${url}/meta/serverlist.html\",\"${url}\"],\"MTR\":[\"${url}/meta/serverlist.html\",\"${url}\"],\"PING\":[\"${url}/meta/serverlist.html\",\"${url}\"],\"TCP\":[\"${url}/meta/serverlist.html\",\"${url}\"]},\"Enable\":true,\"Internet\":\"https://www.baidu.com\"},\"EnableTextReview\":true,\"NicknameReg\":\"^[A-Za-z0-9一-龥]{2,16}$\",\"Passport\":{\"AuthCodeCoolDownDays\":90,\"AuthCodeValidHour\":48,\"DestroyDays\":15,\"ModifyEmailDays\":30,\"ModifyMobileDays\":30,\"Prefix\":\"YS\"},\"PassportPopup\":{\"Enable\":false,\"Text\":\"\"},\"SLS\":{\"AccessKeyID\":\"7b5d0ffd0943f26704fc547a871c68b1b5d56b5c9caeb354205b81f445d7af59\",\"AccessKeySecret\":\"4a5e9cc8a50819290c9bfa1fedc79da7c50e85189a05eb462a3d28a7688eabb0\",\"ENABLE\":true},\"Share\":{\"CaptureScreen\":{\"AutoCloseDelay\":0,\"Enabled\":false},\"PengYouQuan\":{\"AppID\":\"\",\"Enabled\":false,\"UniversalLink\":\"\"},\"QQ\":{\"AppID\":\"\",\"Enabled\":false,\"UniversalLink\":\"\"},\"Qzone\":{\"AppID\":\"\",\"Enabled\":false,\"UniversalLink\":\"\"},\"Sort\":null,\"WeiXin\":{\"AppID\":\"\",\"Enabled\":false,\"UniversalLink\":\"\"},\"Weibo\":{\"AppKey\":\"\",\"Enabled\":false,\"RedirectURL\":\"\",\"UniversalLink\":\"\"}},\"ThirdInfoShareList\":\"https://${url}/cn-nova/shared_list\",\"Version\":{\"ChildPrivacyAgreement\":\"0.1\",\"ErrorCode\":\"5.3\",\"PassportPrivacyAgreement\":\"0.1\",\"PassportUserAgreement\":\"0.1\",\"PrivacyAgreement\":\"0.1\",\"UserAgreement\":\"0.1\",\"UserDestroy\":\"0.1\"}},\"ChannelConfig\":{\"Adjust\":{\"AppID\":\"\",\"Debug\":false,\"Enable\":false,\"EventTokens\":null},\"AiHelp\":{\"DisplayType\":\"WebView\",\"Enable\":true,\"ServiceInterfaceURL\":\"https://customer.yostar.net/\",\"ServiceURL\":\"https://customer-pc.yostar.net/#/\"},\"AntiAddiction\":{\"Enable\":true},\"Debug\":1,\"JPush\":{\"Debug\":false,\"Key\":\"\"},\"Login\":{\"Default\":\"mobile\",\"EnableList\":[\"mobile\",\"taptap\"]},\"OneKeyLoginSecret\":\"5c222d7c997339c921bd4fdcb7549ab4\",\"Taptap\":{\"ClientID\":\"9aeghiagor7hp43smv\",\"ClientPublicKey\":\"MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAwp10sKQjAn+kxSxZebHBgl3Tuqjapxdtqzy68y2CPzELHVeBQpK6jPnHyjvZcpgsW3/Rr4hyzPsxMHp7akNRfkOy2jqEGl/2hDeYpSsvK4vw9triXz4DTLESpU+RWfTglf3LRHB76RJggcGw0Pt+QAItHlOMQH+9LBuWtnS+bcf2YD+kC4jTmvr2dB6i3dhyaorVuT2OfnTRDZREU0WauWdACWHyaUELwA/JfZW+ir88k++qEjxaq76avRXzfJ0SuA8lqJKRirBc4dgMLvpjpjy1mVV3us64fSyAYwqUFCFNT5HT4/MMCbD4YZs20pgT8sUG04kbxlA1qu6sIh2BHwIDAQAB\",\"ClientToken\":\"ENWpPiooc3lI3ZNVNQNOXWZ7dE4YrVOsbMY7b1Li\"},\"Udata\":{\"Enable\":true}}},\"Msg\":\"OK\"}";

    private HttpServer server;

    public CommonConfigHandler(HttpServer server) {
        this.server = server;

        String address = server.getServerConfig().getDisplayAddress();
        this.osConfig = this.osConfig.replaceAll("\\$\\{url}", address);
        this.cnConfig = this.cnConfig.replaceAll("\\$\\{url}", address);
    }

    @Override
    public void handle(@NotNull Context ctx) throws Exception {
        ctx.contentType(ContentType.APPLICATION_JSON);

        String Channel = "";
        var req = JsonUtils.decode(ctx.header("Authorization"), authDataJson.class);

        if (req != null) {
            Channel = req.Head.Channel;
        }

        if (Channel != null && Channel.equals("official")) {
            // cn
            ctx.result(cnConfig);
            return;
        }

        ctx.result(osConfig);
    }

    @SuppressWarnings("unused")
    private static class authDataJson {
        public handJson Head;
        public String Sign;

        protected static class handJson {
            public String Channel;
        }
    }

}
