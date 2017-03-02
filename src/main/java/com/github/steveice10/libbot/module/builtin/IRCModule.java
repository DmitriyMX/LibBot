package com.github.steveice10.libbot.module.builtin;

import org.jibble.pircbot.PircBot;
import com.github.steveice10.libbot.chat.ChatData;
import com.github.steveice10.libbot.module.BotException;
import com.github.steveice10.libbot.module.Module;
import com.github.steveice10.libbot.util.Conditions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Module for connecting to an IRC channel.
 */
public class IRCModule extends PircBot implements Module {
    private String id;
    private String host;
    private int port = -1;
    private String channel;
    private List<ChatData> incoming = new ArrayList<ChatData>();

    /**
     * Creates a new IRCModule instance.
     *
     * @param id       ID of the module.
     * @param username Username to connect with.
     * @param host     Host to connect to.
     * @param channel  Channel to connect to.
     */
    public IRCModule(String id, String username, String host, String channel) {
        Conditions.notNullOrEmpty(id, "Id");
        Conditions.notNullOrEmpty(username, "Username");
        Conditions.notNullOrEmpty(host, "Host");
        Conditions.notNullOrEmpty(channel, "Channel");

        this.setName(username);
        this.id = id;
        this.host = host;
        this.channel = channel;
    }

    /**
     * Creates a new IRCModule instance.
     *
     * @param id       ID of the module.
     * @param username Username to connect with.
     * @param host     Host to connect to.
     * @param port     Port to connect to.
     * @param channel  Channel to connect to.
     */
    public IRCModule(String id, String username, String host, int port, String channel) {
        this(id, username, host, channel);
        this.port = port;
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public void connect() {
        try {
            if(this.port != -1) {
                this.connect(this.host, this.port);
            } else {
                this.connect(this.host);
            }
        } catch(Exception e) {
            throw new BotException("Could not connect to IRC server.", e);
        }

        this.joinChannel(this.channel);
    }

    @Override
    public void disconnect(String reason) {
        if(this.isConnected()) {
            System.out.println("[" + this.id + "] Disconnected: " + reason);
            this.quitServer(reason);
        }
    }

    @Override
    public String getUsername() {
        return this.getNick();
    }

    @Override
    public void setUsername(String name) {
        Conditions.notNullOrEmpty(name, "Username");

        this.changeNick(name);
    }

    @Override
    public List<ChatData> getIncomingChat() {
        if(this.incoming.isEmpty()) {
            return Collections.emptyList();
        }

        List<ChatData> ret = new ArrayList<ChatData>(this.incoming);
        this.incoming.clear();
        return ret;
    }

    @Override
    public void chat(String message) {
        for(String msg : message.split("\n")) {
            this.sendMessage(this.channel, msg);
        }
    }

    @Override
    public void onMessage(String channel, String sender, String login, String hostname, String message) {
        this.incoming.add(new ChatData(sender, message));
    }
}
