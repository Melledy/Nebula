package emu.nebula.server.routes;

import org.jetbrains.annotations.NotNull;

import emu.nebula.GameConstants;
import emu.nebula.proto.Pb.ServerAgent;
import emu.nebula.proto.Pb.ServerListMeta;
import emu.nebula.server.HttpServer;
import emu.nebula.util.AeadHelper;
import io.javalin.http.ContentType;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import lombok.AccessLevel;
import lombok.Getter;

@Getter(AccessLevel.PRIVATE)
public class MetaServerlistHandler implements Handler {
    private HttpServer server;
    private ServerListMeta list;
    private byte[] proto;

    public MetaServerlistHandler(HttpServer server) {
        this.server = server;
        
        // Create server list
        this.list = ServerListMeta.newInstance()
                .setVersion(GameConstants.DATA_VERSION)
                .setReportEndpoint(server.getServerConfig().getDisplayAddress() + "/report");
        
        var agent = ServerAgent.newInstance()
                .setName("Nebula") // TODO allow change in config
                .setAddr(server.getServerConfig().getDisplayAddress() + "/agent-zone-1/")
                .setStatus(1)
                .setZone(1);
        
        this.list.addAgent(agent);
        
        var agent2 = ServerAgent.newInstance()
                .setName("Test") // TODO allow change in config
                .setAddr(server.getServerConfig().getDisplayAddress() + "/agent-zone-1/")
                .setStatus(1)
                .setZone(1);
        
        this.list.addAgent(agent2);
        
        // Cache proto
        this.proto = list.toByteArray();
    }

    @Override
    public void handle(@NotNull Context ctx) throws Exception {
        // Result
        try {
            ctx.contentType(ContentType.APPLICATION_OCTET_STREAM);
            ctx.result(AeadHelper.encryptCBC(this.getProto()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
