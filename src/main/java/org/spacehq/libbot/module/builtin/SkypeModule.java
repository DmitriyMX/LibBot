package org.spacehq.libbot.module.builtin;

import com.skype.*;
import org.spacehq.libbot.chat.ChatData;
import org.spacehq.libbot.module.Module;
import org.spacehq.libbot.module.ModuleException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class SkypeModule implements Module {
	private String chatName;
	private String chatId;
	private Chat chat;

	private List<ChatData> incoming = new CopyOnWriteArrayList<ChatData>();

	public SkypeModule(String chat) {
		this(chat, false);
	}

	public SkypeModule(String chat, boolean name) {
		if(name) {
			this.chatName = chat;
		} else {
			this.chatId = chat;
		}
	}

	@Override
	public void connect() {
		try {
			if(!Skype.isRunning()) {
				throw new ModuleException("Skype is not running.");
			}

			if(this.chatName != null) {
				this.chatId = getChatId(this.chatName);
				this.chatName = null;
			}

			this.chat = null;
			for(Chat chat : Skype.getAllChats()) {
				if(chat.getId().startsWith(this.chatId)) {
					this.chat = chat;
					break;
				}
			}

			if(this.chat == null) {
				throw new ModuleException("Chat \"" + this.chatId + "\" does not exist.");
			}

			Skype.addChatMessageListener(new ChatMessageListener() {
				@Override
				public void chatMessageReceived(ChatMessage chatMessage) throws SkypeException {
					if(chatId.equals(chatMessage.getChat().getId()) && !chatMessage.getContent().trim().isEmpty()) {
						incoming.add(new ChatData(chatMessage.getSender().getFullName(), chatMessage.getContent().trim()));
					}
				}

				@Override
				public void chatMessageSent(ChatMessage chatMessage) throws SkypeException {
					if(chatId.equals(chatMessage.getChat().getId()) && !chatMessage.getContent().trim().isEmpty()) {
						incoming.add(new ChatData(chatMessage.getSender().getFullName(), chatMessage.getContent().trim()));
					}
				}
			});
		} catch(SkypeException e) {
			throw new ModuleException("Failed to connect Skype module.", e);
		}
	}

	@Override
	public void disconnect(String reason) {
		if(this.chat != null) {
			System.out.println(this.getMessagePrefix() + " Disconnected: " + reason);
		}

		this.chat = null;
	}

	@Override
	public String getUsername() {
		try {
			return Skype.getProfile().getFullName();
		} catch(SkypeException e) {
			throw new ModuleException("Failed to get username.", e);
		}
	}

	@Override
	public void setUsername(String name) {
		try {
			Skype.getProfile().setFullName(name);
		} catch(SkypeException e) {
			throw new ModuleException("Failed to set username.", e);
		}
	}

	@Override
	public String getMessagePrefix() {
		return "[Skype]";
	}

	@Override
	public List<ChatData> getIncomingChat() {
		List<ChatData> incoming = new ArrayList<ChatData>(this.incoming);
		this.incoming.removeAll(incoming);
		return incoming;
	}

	@Override
	public void chat(String message) {
		if(this.chat != null) {
			try {
				this.chat.send(message);
			} catch(SkypeException e) {
				throw new ModuleException("Failed to send chat message.", e);
			}
		} else {
			throw new ModuleException("Module not connected.");
		}
	}

	@Override
	public void update() {
	}

	private static String getChatId(String chatName) {
		try {
			for(Chat chat : Skype.getAllChats()) {
				if(chat.getWindowTitle().startsWith(chatName)) {
					return chat.getId();
				}
			}
		} catch(SkypeException e) {
			System.err.println("Failed to get chat ID for chat \"" + chatName + "\".");
			e.printStackTrace();
		}

		return "";
	}
}
