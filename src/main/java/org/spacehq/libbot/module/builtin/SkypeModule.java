package org.spacehq.libbot.module.builtin;

import com.samczsun.skype4j.Skype;
import com.samczsun.skype4j.chat.Chat;
import com.samczsun.skype4j.chat.ChatMessage;
import com.samczsun.skype4j.events.EventHandler;
import com.samczsun.skype4j.events.Listener;
import com.samczsun.skype4j.events.chat.message.MessageEditedEvent;
import com.samczsun.skype4j.events.chat.message.MessageReceivedEvent;
import com.samczsun.skype4j.exceptions.SkypeException;
import com.samczsun.skype4j.formatting.Message;
import com.samczsun.skype4j.formatting.Text;
import org.spacehq.libbot.chat.ChatData;
import org.spacehq.libbot.module.Module;
import org.spacehq.libbot.module.ModuleException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class SkypeModule implements Module {
	private String username;
	private String password;
	private String chatId;

	private Skype skype;
	private Chat chat;

	private List<ChatData> incoming = new CopyOnWriteArrayList<ChatData>();
	private long startTime;

	public SkypeModule(String username, String password, String chatId) {
		if(username == null || username.isEmpty()) {
			throw new IllegalArgumentException("Username cannot be null or empty.");
		}

		if(password == null || password.isEmpty()) {
			throw new IllegalArgumentException("Password cannot be null or empty.");
		}

		if(chatId == null || chatId.isEmpty()) {
			throw new IllegalArgumentException("Chat ID cannot be null or empty.");
		}

		this.username = username;
		this.password = password;
		this.chatId = chatId;
	}

	@Override
	public void connect() {
		this.chat = null;

		try {
			this.skype = Skype.login(this.username, this.password);
			this.skype.subscribe();

			this.chat = this.skype.getChat(this.chatId);
			if(this.chat == null) {
				throw new ModuleException("Chat \"" + this.chatId + "\" does not exist.");
			}

			this.startTime = System.currentTimeMillis();
			this.skype.getEventDispatcher().registerListener(new Listener() {
				@EventHandler
				public void onMessageReceived(MessageReceivedEvent e) {
					receive(e.getMessage());
				}

				@EventHandler
				public void onMessageEdited(MessageEditedEvent e) {
					receive(e.getMessage());
				}
			});
		} catch(ModuleException e) {
			throw e;
		} catch(Exception e) {
			throw new ModuleException("Failed to connect Skype module.", e);
		}
	}

	private void receive(ChatMessage chatMessage) {
		if(this.chat.getIdentity().equals(chatMessage.getChat().getIdentity()) && chatMessage.getTime() > this.startTime) {
			this.incoming.add(new ChatData(chatMessage.getSender().getDisplayName() != null ? chatMessage.getSender().getDisplayName() : chatMessage.getSender().getUsername(), chatMessage.getMessage().asPlaintext().trim()));
		}
	}

	@Override
	public void disconnect(String reason) {
		if(this.skype != null && this.chat != null) {
			System.out.println(this.getMessagePrefix() + " Disconnected: " + reason);
		}

		if(this.skype != null) {
			try {
				this.skype.logout();
			} catch(IOException e) {
			}
		}

		this.chat = null;
		this.skype = null;
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
	public String getMessagePrefix() {
		return "[Skype - " + this.getUsername() + "]";
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
				receive(this.chat.sendMessage(Message.create().with(Text.plain(message))));
			} catch(Exception e) {
				throw new ModuleException("Failed to send chat message.", e);
			}
		} else {
			throw new ModuleException("Module not connected.");
		}
	}

	@Override
	public void update() {
	}
}
