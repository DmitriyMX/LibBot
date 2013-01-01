package ch.spacebase.libbot.module;

import java.util.List;

import ch.spacebase.libbot.chat.ChatData;


public interface Module {

	public void connect();
	
	public void disconnect(String reason);
	
	public String getUsername();
	
	public void setUsername(String name);
	
	public String getMessagePrefix();
	
	public List<ChatData> getIncomingChat();
	
	public void chat(String message);
	
	public void update();
	
}
