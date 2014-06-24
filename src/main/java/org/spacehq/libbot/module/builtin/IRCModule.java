package org.spacehq.libbot.module.builtin;

import org.jibble.pircbot.IrcException;
import org.jibble.pircbot.NickAlreadyInUseException;
import org.jibble.pircbot.PircBot;
import org.spacehq.libbot.chat.ChatData;
import org.spacehq.libbot.module.Module;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class IRCModule extends PircBot implements Module {
	private String host;
	private String channel;
	private List<ChatData> incoming = new ArrayList<ChatData>();

	public IRCModule(String username, String host, String channel) {
		this.setName(username);
		this.host = host;
		this.channel = channel;
	}

	@Override
	public void connect() {
		try {
			this.connect(this.host);
		} catch(NickAlreadyInUseException e) {
			System.err.println("Error connecting to irc server.");
			e.printStackTrace();
			return;
		} catch(IOException e) {
			System.err.println("Error connecting to irc server.");
			e.printStackTrace();
			return;
		} catch(IrcException e) {
			System.err.println("Error connecting to irc server.");
			e.printStackTrace();
			return;
		}

		this.joinChannel(this.channel);
	}

	@Override
	public void disconnect(String reason) {
		if(this.isConnected()) {
			System.out.println(this.getMessagePrefix() + " Disconnected: " + reason);
		}

		this.quitServer(reason);
	}

	@Override
	public String getUsername() {
		return this.getNick();
	}

	@Override
	public void setUsername(String name) {
		this.changeNick(name);
	}

	@Override
	public String getMessagePrefix() {
		return "[IRC]";
	}

	@Override
	public List<ChatData> getIncomingChat() {
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
	public void update() {
	}

	@Override
	public void onMessage(String channel, String sender, String login, String hostname, String message) {
		this.incoming.add(new ChatData(sender, message));
	}
}
