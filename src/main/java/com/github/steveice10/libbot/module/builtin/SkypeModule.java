package com.github.steveice10.libbot.module.builtin;

import com.github.steveice10.libbot.chat.ChatData;
import com.github.steveice10.libbot.module.BotException;
import com.github.steveice10.libbot.module.Module;
import com.github.steveice10.libbot.util.Conditions;
import com.samczsun.skype4j.Skype;
import com.samczsun.skype4j.SkypeBuilder;
import com.samczsun.skype4j.chat.Chat;
import com.samczsun.skype4j.chat.messages.ChatMessage;
import com.samczsun.skype4j.events.EventHandler;
import com.samczsun.skype4j.events.Listener;
import com.samczsun.skype4j.events.chat.message.MessageEditedEvent;
import com.samczsun.skype4j.events.chat.message.MessageReceivedEvent;
import com.samczsun.skype4j.exceptions.ConnectionException;
import com.samczsun.skype4j.exceptions.SkypeException;
import com.samczsun.skype4j.formatting.Message;
import com.samczsun.skype4j.formatting.Text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Logger;

/**
 * Module for connecting to a Skype chat.
 */
public class SkypeModule implements Module {
    static {
        Logger logger = Logger.getLogger("webskype");
        logger.setUseParentHandlers(false);
        for(Handler handler : logger.getHandlers()) {
            logger.removeHandler(handler);
        }
    }

    private String id;
    private String username;
    private String password;
    private String chatId;

    private Skype skype;
    private List<ChatData> incoming = new ArrayList<ChatData>();

    /**
     * Creates a new SkypeModule instance.
     *
     * @param id       ID of the module.
     * @param username Username to connect with.
     * @param password Password to connect with.
     * @param chatId   ID of the chat to connect to.
     */
    public SkypeModule(String id, String username, String password, String chatId) {
        Conditions.notNullOrEmpty(id, "Id");
        Conditions.notNullOrEmpty(username, "Username");
        Conditions.notNullOrEmpty(password, "Password");
        Conditions.notNullOrEmpty(chatId, "Chat ID");

        this.id = id;
        this.username = username;
        this.password = password;
        this.chatId = chatId;
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public boolean isConnected() {
        return this.skype != null;
    }

    @Override
    public void connect() {
        try {
            this.skype = new SkypeBuilder(this.username, this.password).withAllResources().build();
            this.skype.login();
            this.skype.subscribe();

            if(this.skype.getChat(this.chatId) == null) {
                throw new BotException("Chat \"" + this.chatId + "\" does not exist.");
            }

            this.skype.getEventDispatcher().registerListener(new Listener() {
                private long startTime = System.currentTimeMillis();

                @EventHandler
                public void onMessageReceived(MessageReceivedEvent event) {
                    receive(event.getMessage(), event.getMessage().getContent().asPlaintext());
                }

                @EventHandler
                public void onMessageEdited(MessageEditedEvent event) {
                    receive(event.getMessage(), event.getNewContent());
                }

                private void receive(ChatMessage chatMessage, String content) {
                    if(chatId.equals(chatMessage.getChat().getIdentity()) && chatMessage.getSentTime() > this.startTime) {
                        incoming.add(new ChatData(chatMessage.getSender().getUsername(), content.trim()));
                    }
                }
            });
        } catch(BotException e) {
            throw e;
        } catch(Exception e) {
            throw new BotException("Failed to connect Skype module.", e);
        }
    }

    @Override
    public void disconnect(String reason) {
        if(this.skype != null) {
            System.out.println("[" + this.id + "] Disconnected: " + reason);
            try {
                this.skype.logout();
            } catch(ConnectionException e) {
                throw new BotException("Failed to disconnect Skype module.", e);
            } finally {
                this.skype = null;
            }
        }
    }

    @Override
    public String getUsername() {
        if(this.skype != null) {
            return this.skype.getUsername();
        } else {
            return this.username;
        }
    }

    @Override
    public void setUsername(String name) {
        throw new UnsupportedOperationException("Cannot change Skype username.");
    }

    @Override
    public List<ChatData> getIncomingChat() {
        if(this.incoming.isEmpty()) {
            return Collections.emptyList();
        }

        List<ChatData> incoming = new ArrayList<ChatData>(this.incoming);
        this.incoming.removeAll(incoming);
        return incoming;
    }

    @Override
    public void chat(String message) {
        if(this.skype != null) {
            Chat chat = this.skype.getChat(this.chatId);
            if(chat == null) {
                throw new BotException("Chat no longer exists.");
            }

            try {
                chat.sendMessage(Message.create().with(Text.plain(message)));
            } catch(SkypeException e) {
                throw new BotException("Failed to send chat message.", e);
            }
        } else {
            throw new BotException("Module not connected.");
        }
    }
}
