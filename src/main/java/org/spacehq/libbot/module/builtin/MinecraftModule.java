package org.spacehq.libbot.module.builtin;

import org.spacehq.libbot.chat.ChatData;
import org.spacehq.libbot.module.Module;
import org.spacehq.libbot.module.ModuleException;
import org.spacehq.mc.auth.exception.AuthenticationException;
import org.spacehq.mc.protocol.MinecraftProtocol;
import org.spacehq.mc.protocol.data.message.Message;
import org.spacehq.mc.protocol.data.message.TextMessage;
import org.spacehq.mc.protocol.data.message.TranslationMessage;
import org.spacehq.mc.protocol.packet.ingame.client.ClientChatPacket;
import org.spacehq.mc.protocol.packet.ingame.server.ServerChatPacket;
import org.spacehq.packetlib.Client;
import org.spacehq.packetlib.event.session.DisconnectedEvent;
import org.spacehq.packetlib.event.session.PacketReceivedEvent;
import org.spacehq.packetlib.event.session.SessionAdapter;
import org.spacehq.packetlib.tcp.TcpSessionFactory;

import java.util.ArrayList;
import java.util.List;

public class MinecraftModule implements Module {
	private String host;
	private int port;
	private String username;
	private String password;

	private Client conn;
	private List<ChatData> incoming = new ArrayList<ChatData>();

	public MinecraftModule(String host, int port, String username, String password) {
		this.host = host;
		this.port = port;
		this.username = username;
		this.password = password;
	}

	@Override
	public void connect() {
		try {
			this.conn = new Client(this.host, this.port, new MinecraftProtocol(this.username, this.password, false), new TcpSessionFactory());
			this.conn.getSession().addListener(new BotListener());
			this.conn.getSession().connect();
		} catch(AuthenticationException e) {
			throw new ModuleException("Failed to authenticate MinecraftModule.", e);
		}
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
		throw new UnsupportedOperationException("Cannot set name using MinecraftModule.");
	}

	@Override
	public String getMessagePrefix() {
		return "[Minecraft]";
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
			disconnect(Message.fromString(event.getReason()).getFullText());
		}

		private void parseChat(Message message) {
			String user = null;
			String msg = null;
			if(message instanceof TextMessage) {
				String text = message.getText();
				user = text.replaceAll("§[0-9a-z]", "").substring(0, text.indexOf(' '));
				msg = text.replaceAll("§[0-9a-z]", "").substring(text.indexOf(' '));
				// Parse common chat formatting
				user = user.replace("<", "").replace(">", "").replace(" [g]: ", "");
				if(msg.startsWith(": ")) {
					msg = msg.replaceFirst(": ", "");
				}
			} else {
				TranslationMessage trans = (TranslationMessage) message;
				if(trans.getTranslationKey().equals("chat.type.text")) {
					user = trans.getTranslationParams()[0].getFullText();
					msg = trans.getTranslationParams()[1].getFullText();
				}
			}

			if(user != null && msg != null) {
				incoming.add(new ChatData(user, msg));
			}
		}
	}
}
