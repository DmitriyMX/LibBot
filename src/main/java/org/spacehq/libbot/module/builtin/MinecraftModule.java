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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MinecraftModule implements Module {
	private String host;
	private int port;
	private String username;
	private String password;

	private Client conn;
	private List<ChatData> incoming = new ArrayList<ChatData>();
	private List<Pattern> chatPatterns = new ArrayList<Pattern>();

	public MinecraftModule(String host, int port, String username, String password) {
		this.host = host;
		this.port = port;
		this.username = username;
		this.password = password;
		this.addChatPattern("\\<[A-Za-z0-9_-]+\\> (.*)");
	}

	public List<Pattern> getChatPatterns() {
		return new ArrayList<Pattern>(this.chatPatterns);
	}

	public MinecraftModule addChatPattern(String pattern) {
		this.addChatPattern(Pattern.compile(pattern));
		return this;
	}

	public MinecraftModule addChatPattern(Pattern pattern) {
		this.chatPatterns.add(pattern);
		return this;
	}

	public MinecraftModule removeChatPattern(String pattern) {
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

	public MinecraftModule removeChatPattern(Pattern pattern) {
		this.chatPatterns.remove(pattern);
		return this;
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
				String text = message.getText().replaceAll("§[0-9a-z]", "");
				for(Pattern pattern : chatPatterns) {
					Matcher matcher = pattern.matcher(text);
					if(matcher.matches()) {
						user = matcher.group(1);
						msg = matcher.group(2);
						break;
					}
				}
			} else {
				TranslationMessage trans = (TranslationMessage) message;
				if(trans.getTranslationKey().equals("chat.type.text")) {
					user = trans.getTranslationParams()[0].getFullText().replaceAll("§[0-9a-z]", "");
					msg = trans.getTranslationParams()[1].getFullText().replaceAll("§[0-9a-z]", "");
				}
			}

			if(user != null && msg != null) {
				incoming.add(new ChatData(user, msg));
			}
		}
	}
}
