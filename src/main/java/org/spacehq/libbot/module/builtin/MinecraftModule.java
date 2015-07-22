package org.spacehq.libbot.module.builtin;

import org.spacehq.libbot.chat.ChatData;
import org.spacehq.libbot.module.BotException;
import org.spacehq.libbot.module.Module;
import org.spacehq.libbot.util.Conditions;
import org.spacehq.mc.auth.GameProfile;
import org.spacehq.mc.auth.exception.request.RequestException;
import org.spacehq.mc.protocol.MinecraftConstants;
import org.spacehq.mc.protocol.MinecraftProtocol;
import org.spacehq.mc.protocol.data.message.Message;
import org.spacehq.mc.protocol.data.message.TextMessage;
import org.spacehq.mc.protocol.data.message.TranslationMessage;
import org.spacehq.mc.protocol.packet.ingame.client.ClientChatPacket;
import org.spacehq.mc.protocol.packet.ingame.server.ServerChatPacket;
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
 * Module for connecting to a Minecraft server.
 */
public class MinecraftModule implements Module {
    private static final Pattern COLOR_PATTERN = Pattern.compile("(?i)§[0-9A-FK-OR]");

    private String id;
    private String host;
    private int port;
    private String username;
    private String password;

    private List<Pattern> chatPatterns = new ArrayList<Pattern>();

    private Client conn;
    private List<ChatData> incoming = new ArrayList<ChatData>();

    /**
     * Creates a new MinecraftModule instance.
     *
     * @param id       ID of the module.
     * @param username Username to connect with.
     * @param password Password to connect with.
     * @param host     Host of the server to connect to.
     * @param port     Port of the server to connect to.
     */
    public MinecraftModule(String id, String username, String password, String host, int port) {
        Conditions.notNullOrEmpty(id, "Id");
        Conditions.notNullOrEmpty(host, "Host");
        Conditions.notNullOrEmpty(username, "Username");
        Conditions.notNullOrEmpty(password, "Password");

        this.id = id;
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;

        this.addChatPattern("\\<([A-Za-z0-9_-]+)\\> (.*)");
        this.addChatPattern("\\[([A-Za-z0-9_-]+)\\] (.*)");
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
    public MinecraftModule addChatPattern(String pattern) {
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
    public MinecraftModule addChatPattern(Pattern pattern) {
        this.chatPatterns.add(pattern);
        return this;
    }

    /**
     * Removes a chat pattern.
     *
     * @param pattern Pattern to remove.
     * @return This module, for chaining method calls.
     */
    public MinecraftModule removeChatPattern(String pattern) {
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
    public MinecraftModule removeChatPattern(Pattern pattern) {
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
        try {
            this.conn = new Client(this.host, this.port, new MinecraftProtocol(this.username, this.password, false), new TcpSessionFactory());
            this.conn.getSession().addListener(new BotListener());
            this.conn.getSession().connect();
        } catch(RequestException e) {
            throw new BotException("Failed to authenticate MinecraftModule.", e);
        }
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
        if(this.conn == null) {
            return this.username;
        }

        return this.conn.getSession().<GameProfile>getFlag(MinecraftConstants.PROFILE_KEY).getName();
    }

    @Override
    public void setUsername(String name) {
        throw new UnsupportedOperationException("Cannot set name using MinecraftModule.");
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
        List<String> send = new ArrayList<String>();
        for(String msg : message.split("\n")) {
            for(int i = 0; i < msg.length(); i += 100) {
                send.add(msg.substring(i, Math.min(i + 100, msg.length())));
            }
        }

        for(String msg : send) {
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
            disconnect(Message.fromString(event.getReason()).getFullText());
        }

        private void parseChat(Message message) {
            String user = null;
            String msg = null;
            if(message instanceof TextMessage) {
                String text = COLOR_PATTERN.matcher(message.getFullText()).replaceAll("");
                for(Pattern pattern : chatPatterns) {
                    Matcher matcher = pattern.matcher(text);
                    if(matcher.matches()) {
                        user = matcher.group(1);
                        msg = matcher.group(2);
                        break;
                    }
                }
            } else {
                TranslationMessage trans = (TranslationMessage) message;
                if(trans.getTranslationKey().equals("chat.type.text") || trans.getTranslationKey().equals("chat.type.announcement")) {
                    user = COLOR_PATTERN.matcher(trans.getTranslationParams()[0].getFullText()).replaceAll("");
                    msg = COLOR_PATTERN.matcher(trans.getTranslationParams()[1].getFullText()).replaceAll("");
                }
            }

            if(user != null && msg != null) {
                incoming.add(new ChatData(user, msg));
            }
        }
    }
}
