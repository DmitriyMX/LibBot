package org.spacehq.libbot.module.builtin;

import org.spacehq.libbot.chat.ChatData;
import org.spacehq.libbot.module.BotException;
import org.spacehq.libbot.module.Module;
import org.spacehq.libbot.util.Conditions;
import org.spacehq.mc.classic.protocol.exception.AuthenticationException;
import org.spacehq.mc.classic.protocol.ClassicProtocol;
import org.spacehq.mc.classic.protocol.data.serverlist.ServerList;
import org.spacehq.mc.classic.protocol.data.serverlist.ServerURLInfo;
import org.spacehq.mc.classic.protocol.packet.client.ClientChatPacket;
import org.spacehq.mc.classic.protocol.packet.server.ServerChatPacket;
import org.spacehq.packetlib.Client;
import org.spacehq.packetlib.event.session.DisconnectedEvent;
import org.spacehq.packetlib.event.session.PacketReceivedEvent;
import org.spacehq.packetlib.event.session.SessionAdapter;
import org.spacehq.packetlib.tcp.TcpSessionFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Module for connecting to a Minecraft Classic server.
 */
public class MinecraftClassicModule implements Module {
    private static final Pattern COLOR_PATTERN = Pattern.compile("(?i)&[0-9A-FK-OR]");

    private String id;
    private String username;
    private String password;
    private String serverUrl;
    private String verificationKey;
    private String host;
    private int port;

    private List<Pattern> chatPatterns = new ArrayList<Pattern>();

    private Client conn;
    private List<ChatData> incoming = new ArrayList<ChatData>();

    private MinecraftClassicModule(String id, String username) {
        Conditions.notNullOrEmpty(id, "Id");
        Conditions.notNullOrEmpty(username, "Username");

        this.id = id;
        this.username = username;

        this.addChatPattern("\\<([A-Za-z0-9_-]+)\\> (.*)");
        this.addChatPattern("\\[([A-Za-z0-9_-]+)\\] (.*)");
    }

    /**
     * Creates a new MinecraftClassicModule instance.
     *
     * @param id        ID of the module.
     * @param username  Username to connect with.
     * @param password  Password to connect with.
     * @param serverUrl URL of the server to connect to.
     */
    public MinecraftClassicModule(String id, String username, String password, String serverUrl) {
        this(id, username);

        Conditions.notNullOrEmpty(password, "Password");
        Conditions.notNullOrEmpty(serverUrl, "Server URL");

        this.password = password;
        this.serverUrl = serverUrl;
    }

    /**
     * Creates a new MinecraftClassicModule instance.
     *
     * @param id              ID of the module.
     * @param username        Username to connect with.
     * @param verificationKey Verification Key to connect with.
     * @param host            Host of the server to connect to.
     * @param port            Port of the server to connect to.
     */
    public MinecraftClassicModule(String id, String username, String verificationKey, String host, int port) {
        this(id, username);

        Conditions.notNullOrEmpty(verificationKey, "Verification Key");
        Conditions.notNullOrEmpty(host, "Host");

        this.verificationKey = verificationKey;
        this.host = host;
        this.port = port;
    }

    /**
     * Gets a list of chat patterns used to identify chat messages.
     * The first group identifies the username, and the second group identifies the chat message.
     *
     * @return A list of chat patterns used to identify chat messages.
     */
    public List<Pattern> getChatPatterns() {
        return new ArrayList<Pattern>(this.chatPatterns);
    }

    /**
     * Adds a chat pattern for identifying chat messages.
     * The first group should identify the username, and the second group should identify the chat message.
     *
     * @param pattern Pattern to add.
     * @return This module, for chaining method calls.
     */
    public MinecraftClassicModule addChatPattern(String pattern) {
        this.addChatPattern(Pattern.compile(pattern));
        return this;
    }

    /**
     * Adds a chat pattern for identifying chat messages.
     * The first group should identify the username, and the second group should identify the chat message.
     *
     * @param pattern Pattern to add.
     * @return This module, for chaining method calls.
     */
    public MinecraftClassicModule addChatPattern(Pattern pattern) {
        this.chatPatterns.add(pattern);
        return this;
    }

    /**
     * Removes a chat pattern.
     *
     * @param pattern Pattern to remove.
     * @return This module, for chaining method calls.
     */
    public MinecraftClassicModule removeChatPattern(String pattern) {
        Pattern remove = null;
        for(Pattern p : this.chatPatterns) {
            if(p.toString().equals(pattern)) {
                remove = p;
                break;
            }
        }

        if(remove != null) {
            this.removeChatPattern(remove);
        }

        return this;
    }

    /**
     * Removes a chat pattern.
     *
     * @param pattern Pattern to remove.
     * @return This module, for chaining method calls.
     */
    public MinecraftClassicModule removeChatPattern(Pattern pattern) {
        this.chatPatterns.remove(pattern);
        return this;
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public boolean isConnected() {
        return this.conn != null && this.conn.getSession().isConnected();
    }

    @Override
    public void connect() {
        String host;
        int port;
        String username;
        String verificationKey;
        if(this.password != null) {
            try {
                ServerList.login(this.username, this.password);
            } catch(AuthenticationException e) {
                throw new BotException("Failed to authenticate MinecraftClassicModule.", e);
            }

            ServerURLInfo info = ServerList.getServerURLInfo(this.serverUrl);
            host = info.getHost();
            port = info.getPort();
            username = info.getUsername();
            verificationKey = info.getVerificationKey();
        } else {
            host = this.host;
            port = this.port;
            username = this.username.contains("@") ? this.username.substring(0, this.username.indexOf("@")) : this.username;
            verificationKey = this.verificationKey;
        }

        this.conn = new Client(host, port, new ClassicProtocol(username, verificationKey), new TcpSessionFactory());
        this.conn.getSession().addListener(new BotListener());
        this.conn.getSession().connect();
    }

    @Override
    public void disconnect(String reason) {
        if(this.conn != null) {
            System.out.println("[" + this.id + "] Disconnected: " + reason);
            if(this.conn.getSession().isConnected()) {
                this.conn.getSession().disconnect(reason);
            }

            this.conn = null;
        }
    }

    @Override
    public String getUsername() {
        return this.username;
    }

    @Override
    public void setUsername(String name) {
        throw new UnsupportedOperationException("Cannot set name using MinecraftClassicModule.");
    }

    @Override
    public List<ChatData> getIncomingChat() {
        if(this.incoming.isEmpty()) {
            return Collections.emptyList();
        }

        List<ChatData> ret = new ArrayList<ChatData>(this.incoming);
        this.incoming.removeAll(ret);
        return ret;
    }

    @Override
    public void chat(String message) {
        for(String msg : message.split("\n")) {
            this.conn.getSession().send(new ClientChatPacket(msg));
        }
    }

    private class BotListener extends SessionAdapter {
        @Override
        public void packetReceived(PacketReceivedEvent event) {
            if(event.getPacket() instanceof ServerChatPacket) {
                this.parseChat(event.<ServerChatPacket>getPacket().getMessage());
            }
        }

        @Override
        public void disconnected(DisconnectedEvent event) {
            disconnect(event.getReason());
        }

        private void parseChat(String message) {
            String text = COLOR_PATTERN.matcher(message).replaceAll("");
            String user = null;
            String msg = null;
            for(Pattern pattern : chatPatterns) {
                Matcher matcher = pattern.matcher(text);
                if(matcher.matches()) {
                    user = matcher.group(1);
                    msg = matcher.group(2);
                    break;
                }
            }

            if(user != null && msg != null) {
                incoming.add(new ChatData(user, msg));
            }
        }
    }
}
