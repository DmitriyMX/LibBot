package org.spacehq.libbot.module.builtin;

import com.samczsun.skype4j.Skype;
import com.samczsun.skype4j.chat.Chat;
import com.samczsun.skype4j.chat.ChatMessage;
import com.samczsun.skype4j.events.EventHandler;
import com.samczsun.skype4j.events.Listener;
import com.samczsun.skype4j.events.chat.message.MessageEditedByOtherEvent;
import com.samczsun.skype4j.events.chat.message.MessageEditedEvent;
import com.samczsun.skype4j.events.chat.message.MessageReceivedEvent;
import com.samczsun.skype4j.formatting.Message;
import com.samczsun.skype4j.formatting.Text;
import org.spacehq.libbot.chat.ChatData;
import org.spacehq.libbot.module.BotException;
import org.spacehq.libbot.module.Module;
import org.spacehq.libbot.util.Conditions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Module for connecting to a Skype chat.
 */
public class SkypeModule implements Module {
    private String id;
    private String username;
    private String password;
    private String chatId;

    private Skype skype;
    private Chat chat;

    private List<ChatData> incoming = new CopyOnWriteArrayList<ChatData>();
    private long startTime;

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
        return this.skype != null && this.chat != null;
    }

    @Override
    public void connect() {
        this.chat = null;

        try {
            this.skype = Skype.login(this.username, this.password);
            this.skype.subscribe();

            this.chat = this.skype.getChat(this.chatId);
            if(this.chat == null) {
                throw new BotException("Chat \"" + this.chatId + "\" does not exist.");
            }

            this.startTime = System.currentTimeMillis();
            this.skype.getEventDispatcher().registerListener(new Listener() {
                @EventHandler
                public void onMessageReceived(MessageReceivedEvent e) {
                    receive(e.getMessage(), e.getMessage().getMessage().asPlaintext());
                }

                @EventHandler
                public void onMessageEdited(MessageEditedEvent e) {
                    receive(e.getMessage(), e.getMessage().getMessage().asPlaintext());
                }

                @EventHandler
                public void onMessageEditedByOther(MessageEditedByOtherEvent e) {
                    receive(e.getMessage(), e.getNewContent());
                }
            });
        } catch(BotException e) {
            throw e;
        } catch(Exception e) {
            throw new BotException("Failed to connect Skype module.", e);
        }
    }

    private void receive(ChatMessage chatMessage, String content) {
        if(this.chat.getIdentity().equals(chatMessage.getChat().getIdentity()) && chatMessage.getTime() > this.startTime) {
            this.incoming.add(new ChatData(chatMessage.getSender().getDisplayName() != null ? chatMessage.getSender().getDisplayName() : chatMessage.getSender().getUsername(), content.trim()));
        }
    }

    @Override
    public void disconnect(String reason) {
        if(this.skype != null) {
            System.out.println("[" + this.id + "] Disconnected: " + reason);
            this.chat = null;

            try {
                this.skype.logout();
                this.skype = null;
            } catch(IOException e) {
                this.skype = null;
                throw new BotException("Failed to disconnect Skype module.", e);
            }
        }
    }

    @Override
    public String getUsername() {
        if(this.skype != null && this.chat != null) {
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
        List<ChatData> incoming = new ArrayList<ChatData>(this.incoming);
        this.incoming.removeAll(incoming);
        return incoming;
    }

    @Override
    public void chat(String message) {
        if(this.skype != null && this.chat != null) {
            try {
                ChatMessage msg = this.chat.sendMessage(Message.create().with(Text.plain(message)));
                receive(msg, msg.getMessage().asPlaintext());
            } catch(IllegalArgumentException e) {
                // TODO: Stop this from happening ("User must not be null" internally). Until then, swallow these exceptions.
            } catch(Exception e) {
                throw new BotException("Failed to send chat message.", e);
            }
        } else {
            throw new BotException("Module not connected.");
        }
    }

    @Override
    public void update() {
    }
}
