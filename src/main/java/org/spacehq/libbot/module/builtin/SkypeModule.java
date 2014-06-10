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
	private Chat chat;

	private List<ChatData> incoming = new CopyOnWriteArrayList<ChatData>();

	public SkypeModule(String chat) {
		this.chatName = chat;
	}

	@Override
	public void connect() {
		try {
			if(!Skype.isRunning()) {
				throw new ModuleException("Skype is not running.");
			}

			Skype.addChatMessageListener(new ChatMessageListener() {
				@Override
				public void chatMessageReceived(ChatMessage chatMessage) throws SkypeException {
					if(chatName.equals(chatMessage.getChat().getWindowTitle())) {
						incoming.add(new ChatData(chatMessage.getSender().getFullName(), chatMessage.getContent()));
					}
				}

				@Override
				public void chatMessageSent(ChatMessage chatMessage) throws SkypeException {
					if(chatName.equals(chatMessage.getChat().getWindowTitle())) {
						incoming.add(new ChatData(chatMessage.getSender().getFullName(), chatMessage.getContent()));
					}
				}
			});

			this.chat = null;
			for(Chat chat : Skype.getAllChats()) {
				if(chat.getWindowTitle().startsWith(this.chatName)) {
					this.chat = chat;
					break;
				}
			}

			if(this.chat == null) {
				throw new ModuleException("Chat \"" + this.chatName + "\" does not exist.");
			}
		} catch(SkypeException e) {
			throw new ModuleException("Failed to connect Skype module.", e);
		}
	}

	@Override
	public void disconnect(String reason) {
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
		throw new UnsupportedOperationException("Cannot set name using SkypeModule.");
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
				for(String msg : message.split("\n")) {
					this.chat.send(msg);
				}
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
}
