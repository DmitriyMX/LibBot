package org.spacehq.libbot.module;

import org.spacehq.libbot.chat.ChatData;

import java.util.List;

public interface Module {
	public boolean isConnected();

	public void connect();

	public void disconnect(String reason);

	public String getUsername();

	public void setUsername(String name);

	public String getMessagePrefix();

	public List<ChatData> getIncomingChat();

	public void chat(String message);

	public void update();
}
