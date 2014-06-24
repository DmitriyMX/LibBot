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

public class MinecraftClassicModule implements Module {
	private String username;
	private String password;
	private String serverUrl;
	private String verificationKey;
	private String host;
	private int port;

	private Client conn;
	private List<ChatData> incoming = new ArrayList<ChatData>();

	public MinecraftClassicModule(String username, String password, String serverUrl) {
		this.username = username;
		this.password = password;
		this.serverUrl = serverUrl;
	}

	public MinecraftClassicModule(String username, String verificationKey, String host, int port) {
		this.username = username;
		this.verificationKey = verificationKey;
		this.host = host;
		this.port = port;
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
		return "[MinecraftClassic]";
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
			// Try to parse common chat formatting
			String text = message.replaceAll("&[0-9a-z]", "");
			int start = text.startsWith("[") ? text.indexOf("]") + 1 : 0;
			String user = text.substring(start, text.indexOf(' ', start)).trim();
			String msg = text.substring(text.indexOf(' ', start)).trim();
			user = user.replace("<", "").replace(">", "").replace(" [g]: ", "");
			if(msg.startsWith(": ")) {
				msg = msg.replaceFirst(": ", "");
			}

			incoming.add(new ChatData(user, msg));
		}
	}
}
