package ch.spacebase.libbot.module.builtin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jibble.pircbot.IrcException;
import org.jibble.pircbot.NickAlreadyInUseException;
import org.jibble.pircbot.PircBot;

import ch.spacebase.libbot.Bot;
import ch.spacebase.libbot.LibraryInfo;
import ch.spacebase.libbot.chat.ChatData;
import ch.spacebase.libbot.module.Module;


public class IRCModule extends PircBot implements Module {

	private Bot bot;
	private String host;
	private String channel;
	private List<ChatData> incoming = new ArrayList<ChatData>();
	
	public IRCModule(Bot bot, String username, String host, String channel) {
		this.bot = bot;
		this.setName(username);
		this.host = host;
		this.channel = channel;
	}
	
	@Override
	public void connect() {
		try {
			this.connect(this.host);
		} catch (NickAlreadyInUseException e) {
			System.err.println("Error connecting to irc server.");
			e.printStackTrace();
			return;
		} catch (IOException e) {
			System.err.println("Error connecting to irc server.");
			e.printStackTrace();
			return;
		} catch (IrcException e) {
			System.err.println("Error connecting to irc server.");
			e.printStackTrace();
			return;
		}
		
		this.joinChannel(this.channel);
		this.chat(this.bot.getName() + " v" + this.bot.getVersion() + " connected.");
		this.chat("Using " + LibraryInfo.NAME + " v" + LibraryInfo.VERSION + ".");
	}

	@Override
	public void disconnect(String reason) {
		System.out.println(this.getMessagePrefix() + " Disconnected: " + reason);
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
		this.sendMessage(this.channel, "[bot] " + message);
	}

	@Override
	public void update() {
	}
	
	@Override
	public void onMessage(String channel, String sender, String login, String hostname, String message) {
		this.incoming.add(new ChatData(sender, message));
	}

}
