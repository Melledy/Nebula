package emu.nebula.server;

import java.io.File;
import java.io.FileReader;

import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import emu.nebula.Config.HttpServerConfig;
import emu.nebula.GameConstants;
import emu.nebula.Nebula;
import emu.nebula.Nebula.ServerType;
import emu.nebula.proto.Pb.ClientDiff;
import emu.nebula.server.routes.*;
import emu.nebula.util.JsonUtils;
import io.javalin.Javalin;
import io.javalin.http.ContentType;
import io.javalin.http.Context;
import io.javalin.http.staticfiles.Location;
import lombok.Getter;

@Getter
public class HttpServer {
    private final Javalin app;
    private ServerType type;
    private boolean started;

    // Cached client diff
    private PatchList patchlist;
    private byte[] diff;

    public HttpServer(ServerType type) {
        this.type = type;
        this.app = Javalin.create(javalinConfig -> {
            var staticFilesDir = new File(Nebula.getConfig().getWebFilesDir());
            if (staticFilesDir.exists()) {
                javalinConfig.staticFiles.add(staticFilesDir.getPath(), Location.EXTERNAL);
            }
        });

        this.loadPatchList();
        this.addRoutes();
    }

    public HttpServerConfig getServerConfig() {
        return Nebula.getConfig().getHttpServer();
    }

    private HttpConnectionFactory getHttpFactory() {
        var httpsConfig = new HttpConfiguration();
        var src = new SecureRequestCustomizer();
        src.setSniHostCheck(false);
        httpsConfig.addCustomizer(src);
        return new HttpConnectionFactory(httpsConfig);
    }

    private SslContextFactory.Server getSSLContextFactory() {
        var sslContextFactory = new SslContextFactory.Server();
        sslContextFactory.setKeyStorePath(Nebula.getConfig().getKeystore().getPath());
        sslContextFactory.setKeyStorePassword(Nebula.getConfig().getKeystore().getPassword());
        sslContextFactory.setSniRequired(false);
        sslContextFactory.setRenegotiationAllowed(false);
        return sslContextFactory;
    }

    // Patch list

    public long getDataVersion() {
        return getPatchlist() != null ? getPatchlist().getVersion() : GameConstants.getDataVersion();
    }

    public synchronized void loadPatchList() {
        // Clear
        this.patchlist = null;
        this.diff = null;

        // Get file
        File file = new File(Nebula.getConfig().getPatchListPath());

        if (!file.exists()) {
            this.diff = ClientDiff.newInstance().toByteArray();
            return;
        }

        // Load
        try (FileReader reader = new FileReader(file)) {
            this.patchlist = JsonUtils.loadToClass(reader, PatchList.class);
            this.diff = patchlist.toProto().toByteArray();
        } catch (Exception e) {
            this.patchlist = null;
            this.diff = ClientDiff.newInstance().toByteArray();
        }

        if (this.patchlist != null) {
            Nebula.getLogger().info("Loaded patchlist (Data version: " + patchlist.getVersion() + ")");
        }
    }

    // Start server

    public void start() {
        if (this.started) {
            return;
        }
            
        this.started = true;

        // Http server
        if (getServerConfig().isUseSSL()) {
            ServerConnector sslConnector = new ServerConnector(getApp().jettyServer().server(), getSSLContextFactory(), getHttpFactory());
            sslConnector.setHost(getServerConfig().getBindAddress());
            sslConnector.setPort(getServerConfig().getBindPort());
            getApp().jettyServer().server().addConnector(sslConnector);
            getApp().start();
        } else {
            getApp().start(getServerConfig().getBindAddress(), getServerConfig().getBindPort());
        }
        
        if (type.runGame()) {
            Nebula.getLogger().info("Nebula PS is free server software.");
            Nebula.getLogger().info("Github: https://github.com/Melledy/Nebula");
            Nebula.getLogger().info("Discord: https://discord.gg/cskCWBqdJk");
        }
        
        // Done
        Nebula.getLogger().info("Http Server started on " + getServerConfig().getBindPort());
    }

    // Server endpoints

    private void addRoutes() {

        // Add routes
        if (this.getType().runLogin()) {
            this.addLoginServerRoutes();
        }

        if (this.getType().runGame()) {
            this.addGameServerRoutes();
        }
        
        // Custom api route(s)
        getApp().post("/api/command", new RemoteHandler());

        // Exception handler
        getApp().exception(Exception.class, (e, c) -> {
            e.printStackTrace();
        });

        // Fallback handler
        getApp().error(404, this::notFoundHandler);
    }

    private void addLoginServerRoutes() {
        // https://en-sdk-api.yostarplat.com/
        getApp().post("/common/config", new CommonConfigHandler(this));
        getApp().post("/common/client-code", new CommonClientCodeHandler());
        getApp().post("/common/version", new HttpJsonResponse("{\"Code\":200,\"Data\":{\"Agreement\":[{\"Version\":\"0.1\",\"Type\":\"user_agreement\",\"Title\":\"用户协议\",\"Content\":\"\",\"Lang\":\"en\"},{\"Version\":\"0.1\",\"Type\":\"privacy_agreement\",\"Title\":\"隐私政策\",\"Content\":\"\",\"Lang\":\"en\"}],\"ErrorCode\":\"4.4\"},\"Msg\":\"OK\"}"));

        getApp().post("/user/detail", new UserLoginHandler());
        getApp().post("/user/set", new UserSetDataHandler());
        getApp().post("/user/login", new UserLoginHandler());
        getApp().post("/user/quick-login", new UserLoginHandler());
        getApp().post("/user/set-info", new HttpJsonResponse("{\"Code\":200,\"Data\":{},\"Msg\":\"OK\"}"));
        getApp().post("/user/send-sms", new HttpJsonResponse("{\"Code\":200,\"Data\":{},\"Msg\":\"OK\"}"));

        getApp().post("/yostar/get-auth", new GetAuthHandler());
        getApp().post("/yostar/send-code", new HttpJsonResponse("{\"Code\":200,\"Data\":{},\"Msg\":\"OK\"}")); // Dummy handler

        // https://nova-static.stellasora.global/
        getApp().get("/meta/serverlist.html", new MetaServerlistHandler(this));

        /*
           fishiatee: Maybe this should be handled better.

           For example, if raw meta is detected in say ./web/meta, serve that instead.
           Otherwise, detect and serve from custom patchlist definition.
        */
        //getApp().get("/meta/*.html", new MetaPatchListHandler(this));
    }

    private void addGameServerRoutes() {
        getApp().post("/agent-zone-1/", new AgentZoneHandler());
    }

    private void notFoundHandler(Context ctx) {
        ctx.status(404);
        ctx.contentType(ContentType.APPLICATION_JSON);
        ctx.result("{}");
    }
}
