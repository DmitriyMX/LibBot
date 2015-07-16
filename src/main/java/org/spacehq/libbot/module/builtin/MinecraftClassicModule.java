package org.spacehq.libbot.module.builtin;

import org.spacehq.libbot.chat.ChatData;
import org.spacehq.libbot.module.Module;
import org.spacehq.libbot.module.ModuleException;
import org.spacehq.mc.classic.protocol.AuthenticationException;
import org.spacehq.mc.classic.protocol.ClassicProtocol;
import org.spacehq.mc.classic.protocol.data.serverlist.ServerList;
import org.spacehq.mc.classic.protocol.data.serverlist.ServerURLInfo;
import org.spacehq.mc.classic.protocol.packet.client.ClientChatPacket;
import org.spacehq.mc.classic.protocol.packet.server.ServerChatPacket;
import org.spacehq.packetlib.Client;
import org.spacehq.packetlib.event.session.DisconnectedEvent;
import org.spacehq.packetlib.event.session.PacketReceivedEvent;
import org.spacehq.packetlib.event.session.SessionAdapter;
import org.spacehq.packetlib.tcp.TcpSessionFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MinecraftClassicModule implements Module {
	private static final Pattern COLOR_PATTERN = Pattern.compile("(?i)&[0-9A-FK-OR]");

	private String username;
	private String password;
	private String serverUrl;
	private String verificationKey;
	private String host;
	private int port;

	private Client conn;
	private List<ChatData> incoming = new ArrayList<ChatData>();
	private List<Pattern> chatPatterns = new ArrayList<Pattern>();

	private MinecraftClassicModule() {
		this.addChatPattern("\\<([A-Za-z0-9_-]+)\\> (.*)");
		this.addChatPattern("\\[([A-Za-z0-9_-]+)\\] (.*)");
	}

	public MinecraftClassicModule(String username, String password, String serverUrl) {
		this();

		if(username == null || username.isEmpty()) {
			throw new IllegalArgumentException("Username cannot be null or empty.");
		}

		if(password == null || password.isEmpty()) {
			throw new IllegalArgumentException("Password cannot be null or empty.");
		}

		if(serverUrl == null || serverUrl.isEmpty()) {
			throw new IllegalArgumentException("Server URL cannot be null or empty.");
		}

		this.username = username;
		this.password = password;
		this.serverUrl = serverUrl;
	}

	public MinecraftClassicModule(String username, String verificationKey, String host, int port) {
		this();

		if(username == null || username.isEmpty()) {
			throw new IllegalArgumentException("Username cannot be null or empty.");
		}

		if(verificationKey == null || verificationKey.isEmpty()) {
			throw new IllegalArgumentException("Verification Key cannot be null or empty.");
		}

		if(host == null || host.isEmpty()) {
			throw new IllegalArgumentException("Host cannot be null or empty.");
		}

		this.username = username;
		this.verificationKey = verificationKey;
		this.host = host;
		this.port = port;
	}

	public List<Pattern> getChatPatterns() {
		return new ArrayList<Pattern>(this.chatPatterns);
	}

	public MinecraftClassicModule addChatPattern(String pattern) {
		this.addChatPattern(Pattern.compile(pattern));
		return this;
	}

	public MinecraftClassicModule addChatPattern(Pattern pattern) {
		this.chatPatterns.add(pattern);
		return this;
	}

	public MinecraftClassicModule removeChatPattern(String pattern) {
		Pattern remove = null;
		for(Pattern p : this.chatPatterns) {
			if(p.toString().equals(pattern)) {
				remove = p;
				break;
			}
		}

		if(remove != null) {
			this.removeChatPattern(remove);
		}

		return this;
	}

	public MinecraftClassicModule removeChatPattern(Pattern pattern) {
		this.chatPatterns.remove(pattern);
		return this;
	}

	@Override
	public void connect() {
		String host;
		int port;
		String username;
		String verificationKey;
		if(this.password != null) {
			try {
				ServerList.login(this.username, this.password);
			} catch(AuthenticationException e) {
				throw new ModuleException("Failed to authenticate MinecraftClassicModule.", e);
			}

			ServerURLInfo info = ServerList.getServerInfo(this.serverUrl);
			host = info.getHost();
			port = info.getPort();
			username = info.getUsername();
			verificationKey = info.getVerificationKey();
		} else {
			host = this.host;
			port = this.port;
			username = this.username.contains("@") ? this.username.substring(0, this.username.indexOf("@")) : this.username;
			verificationKey = this.verificationKey;
		}

		this.conn = new Client(host, port, new ClassicProtocol(username, verificationKey), new TcpSessionFactory());
		this.conn.getSession().addListener(new BotListener());
		this.conn.getSession().connect();
	}

	@Override
	public void disconnect(String reason) {
		if(this.conn != null) {
			System.out.println(this.getMessagePrefix() + " Disconnected: " + reason);
			if(this.conn.getSession().isConnected()) {
				this.conn.getSession().disconnect(reason);
			}
		}

		this.conn = null;
	}

	@Override
	public String getUsername() {
		return this.username;
	}

	@Override
	public void setUsername(String name) {
		throw new UnsupportedOperationException("Cannot set name using MinecraftClassicModule.");
	}

	@Override
	public String getMessagePrefix() {
		return "[MinecraftClassic - " + this.username + " - " + (this.serverUrl != null ? this.serverUrl : (this.host + ":" + this.port)) + "]";
	}

	@Override
	public List<ChatData> getIncomingChat() {
		List<ChatData> ret = new ArrayList<ChatData>(this.incoming);
		this.incoming.removeAll(ret);
		return ret;
	}

	@Override
	public void chat(String message) {
		for(String msg : message.split("\n")) {
			this.conn.getSession().send(new ClientChatPacket(msg));
		}
	}

	@Override
	public void update() {
	}

	private class BotListener extends SessionAdapter {
		@Override
		public void packetReceived(PacketReceivedEvent event) {
			if(event.getPacket() instanceof ServerChatPacket) {
				this.parseChat(event.<ServerChatPacket>getPacket().getMessage());
			}
		}

		@Override
		public void disconnected(DisconnectedEvent event) {
			disconnect(event.getReason());
		}

		private void parseChat(String message) {
			String text = COLOR_PATTERN.matcher(message).replaceAll("");
			String user = null;
			String msg = null;
			for(Pattern pattern : chatPatterns) {
				Matcher matcher = pattern.matcher(text);
				if(matcher.matches()) {
					user = matcher.group(1);
					msg = matcher.group(2);
					break;
				}
			}

			if(user != null && msg != null) {
				incoming.add(new ChatData(user, msg));
			}
		}
	}
}
