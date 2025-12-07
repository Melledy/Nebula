package emu.nebula.server.handlers;

import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.PlayerLogin.LoginReq;
import emu.nebula.proto.PlayerLogin.LoginResp;
import emu.nebula.net.HandlerId;
import emu.nebula.Nebula;
import emu.nebula.net.GameSession;

@HandlerId(NetMsgId.player_login_req)
public class HandlerPlayerLoginReq extends NetHandler {

    public boolean requirePlayer() {
        return false;
    }

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        // Parse request
        var req = LoginReq.parseFrom(message);

        // os
        String loginToken = req.getOfficialOverseas().getToken();

        if (loginToken == null || loginToken.isEmpty()) {
            // cn
            loginToken = req.getOfficial().getToken();
        }


        // Login
        boolean result = session.login(loginToken);

        if (!result) {
            return session.encodeMsg(NetMsgId.player_login_failed_ack);
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
