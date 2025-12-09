package emu.nebula.server.routes;

import java.lang.reflect.Field;
import java.util.Set;

import org.reflections.Reflections;

import emu.nebula.Nebula;
import emu.nebula.game.GameContext;
import emu.nebula.net.*;
import emu.nebula.util.AeadHelper;
import emu.nebula.util.Utils;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.Getter;

@Getter
public class AgentZoneHandler implements Handler {
    private final Int2ObjectMap<NetHandler> handlers;
    private final static byte[] EMPTY_BYTES = new byte[0];

    public AgentZoneHandler() {
        this.handlers = new Int2ObjectOpenHashMap<>();
        this.registerHandlers();
        this.registerDummyHandlers();
    }
    
    protected GameContext getGameContext() {
        return Nebula.getGameContext();
    }
    
    @Override
    public void handle(Context ctx) throws Exception {
        // Setup session
        GameSession session = null;
        
        byte[] sessionKey = AeadHelper.serverGarbleKey;
        boolean hasKey3 = false;
        int encryptMethod = 0;
        
        // Get token
        String token = ctx.header("X-Token");
        
        // Set headers
        ctx.res().setHeader("Server", "agent");
        
        // Check if we have a token
        if (token != null) {
            // Get session
            session = getGameContext().getSessionByToken(token);
            
            // Uh oh - session not found
            if (session == null || session.getKey() == null) {
                ctx.status(500);
                ctx.result("");
                return;
            }
            
            // Set key
            sessionKey = session.getKey();
            encryptMethod = session.getEncryptMethod();
            hasKey3 = true;
        }
        
        // Parse request
        byte[] data = null;
        int msgId = 0;
        
        try {
            // Get message
            byte[] message = ctx.bodyAsBytes();
            int offset = 0;
            
            // Sanity for malformed packets
            if (message.length <= 12) {
                ctx.status(500);
                ctx.result("");
                return;
            }
            
            // Decrypt message
            if (hasKey3) {
                message = AeadHelper.decrypt(message, sessionKey, encryptMethod);
                offset = 10;
            } else {
                message = AeadHelper.decryptBasic(message, sessionKey);
                message = AeadHelper.decryptGCM(message, sessionKey);
            }
            
            // Get message id
            msgId = (message[offset++] << 8) | (message[offset++] & 0xff);
            
            // Set data
            data = new byte[message.length - offset];
            System.arraycopy(message, offset, data, 0, data.length);
            
            // Log
            if (Nebula.getConfig().getLogOptions().packets) {
                this.logRecv(msgId, data);
            }
        } catch (Exception e) {
            // Decrypt error
            e.printStackTrace();
            ctx.status(500);
            ctx.result("");
            return;
        }
        
        // Update last active time for session
        if (session != null) {
            session.updateLastActiveTime();
            session.updateIpAddress(ctx.ip());
        }
        
        // Handle packet
        NetHandler handler = this.handlers.get(msgId);
        byte[] result = null;
    
        try {
            if (handler == null) {
                Nebula.getLogger().warn("Unhandled request: " + msgId);
                return;
            }
            
            // Check handler requirements
            if (session == null) {
                if (handler.requireSession()) {
                    return;
                }
            } else if (session.getPlayer() == null && handler.requirePlayer()) {
                return;
            }
            
            // Handle data
            result = handler.handle(session, data);
        } catch (Exception e) {
            // Handler error
            e.printStackTrace();
        } finally {
            // Send result
            if (result == null) {
                ctx.status(500);
                ctx.result(EMPTY_BYTES);
                return;
            }
            
            // Log
            if (Nebula.getConfig().getLogOptions().packets) {
                this.logSend(result);
            }
            
            // Encrypt
            if (hasKey3) {
                result = AeadHelper.encrypt(result, sessionKey, encryptMethod);
            } else {
                result = AeadHelper.encryptGCM(result, sessionKey);
                result = AeadHelper.encryptBasic(result, sessionKey);
            }
            
            // Send to client
            ctx.status(200);
            ctx.result(result);
            
            // Clear header
            ctx.res().setHeader("Content-Type", null);
            
            // Handle post response events
            if (session != null) {
                session.afterResponse();
            }
        }
    }
    
    // Loggers
    
    private void logRecv(int msgId, byte[] data) {
        Nebula.getLogger().info("RECV: " + NetMsgIdUtils.getMsgIdName(msgId) + " (" + msgId + ")");
        System.out.println(Utils.bytesToHex(data));
    }
    
    private void logSend(byte[] data) {
        int sendMsgId = (data[0] << 8) | (data[1] & 0xff);
        Nebula.getLogger().info("SEND: " + NetMsgIdUtils.getMsgIdName(sendMsgId) + " (" + sendMsgId + ")");
        System.out.println(Utils.bytesToHex(data, 2));
    }
    
    // Register handlers

    private void registerHandlers() {
        // Setup handlers
        Reflections reflections = new Reflections(Nebula.class.getPackageName());
        Set<Class<?>> handlers = reflections.getTypesAnnotatedWith(HandlerId.class);

        for (Class<?> cls : handlers) {
            // Make sure class is a handler
            if (!NetHandler.class.isAssignableFrom(cls)) {
                continue;
            }

            try {
                NetHandler handler = (NetHandler) cls.getConstructor().newInstance();
                HandlerId def = cls.getAnnotation(HandlerId.class);
                
                int opcode = def.value();
                if (opcode != 0) {
                    // Put in handler map
                    this.handlers.put(opcode, handler);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        // Log registration
        Nebula.getLogger().info("Registered " + handlers.size() + " handlers");
    }
    
    private void registerDummyHandlers() {
        int count = 0;
        Field[] fields = NetMsgId.class.getFields();

        for (Field f : fields) {
            if (!f.getType().equals(int.class)) {
                continue;
            }
            
            try {
                // Get name + msgId
                String name = f.getName();
                int msgId = f.getInt(null);
                
                // Skip if we already have an handler
                if (this.getHandlers().containsKey(msgId)) {
                    continue;
                }
                
                if (name.endsWith("_req")) {
                    // Find failed rsp msg id
                    Field failedAckField = NetMsgId.class.getField(name.substring(0, name.length() - 3) + "failed_ack");
                    if (failedAckField == null) {
                        continue;
                    }
                    
                    int failedAckId = failedAckField.getInt(null);
                    
                    // Create dummy handler
                    var handler = new NetHandler() {
                        @Override
                        public byte[] handle(GameSession session, byte[] message) throws Exception {
                            return PacketHelper.encodeMsg(failedAckId);
                        }
                    };
                    
                    // Add
                    this.handlers.put(msgId, handler);
                    
                    // Increment count
                    count++;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        // Log registration
        Nebula.getLogger().info("Registered " + count + " dummy handlers");
    }
}

