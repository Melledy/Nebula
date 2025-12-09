package emu.nebula.server.handlers;

import emu.nebula.Nebula;
import emu.nebula.game.player.Player;
import emu.nebula.game.player.PlayerErrorCode;
import emu.nebula.net.GameSession;
import emu.nebula.net.HandlerId;
import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.PlayerLogin.LoginReq;
import emu.nebula.proto.PlayerLogin.LoginResp;
import emu.nebula.proto.Public.Error;

@HandlerId(NetMsgId.player_login_req)
public class HandlerPlayerLoginReq extends NetHandler {

    public boolean requirePlayer() {
        return false;
    }

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        // Parse request
        var req = LoginReq.parseFrom(message);

        // OS
        String loginToken = req.getOfficialOverseas().getToken();

        if (loginToken.isEmpty()) {
            // CN
            loginToken = req.getOfficial().getToken();
        }


        var banModule = Nebula.getGameContext().getBanModule();

        // Check IP ban
        if (banModule.isIpBanned(session.getIpAddress())) {
            var banInfo = banModule.getIpBanInfo(session.getIpAddress());
            return session.encodeMsg(NetMsgId.player_login_failed_ack, banInfo.toProto());
        }

        // Login
        boolean result = session.login(loginToken);
        if (!result) {
            Error errorCause = Error.newInstance().setCode(PlayerErrorCode.ErrLogin.getValue());
            return session.encodeMsg(NetMsgId.player_login_failed_ack, errorCause);
        }

        // Check player ban
        int playerUid = session.getPlayer().getUid();
        if (banModule.isPlayerBanned(playerUid)) {
            var banInfo = banModule.getPlayerBanInfo(playerUid);
            return session.encodeMsg(NetMsgId.player_login_failed_ack, banInfo.toProto());
        }

        // Regenerate session token because we are switching encrpytion method
        Nebula.getGameContext().generateSessionToken(session);

        // Create rsp
        var rsp = LoginResp.newInstance()
                .setToken(session.getToken());

        // Encode and send to client
        return session.encodeMsg(NetMsgId.player_login_succeed_ack, rsp);
    }

}
