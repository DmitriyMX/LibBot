package ch.spacebase.libbot.module.builtin;

import java.util.ArrayList;
import java.util.List;

import ch.spacebase.libbot.Bot;
import ch.spacebase.libbot.LibraryInfo;
import ch.spacebase.libbot.chat.ChatData;
import ch.spacebase.libbot.module.Module;
import ch.spacebase.mc.auth.exceptions.AuthenticationException;
import ch.spacebase.mc.protocol.MinecraftProtocol;
import ch.spacebase.mc.protocol.packet.ingame.client.ClientChatPacket;
import ch.spacebase.mc.protocol.packet.ingame.server.ServerChatPacket;
import ch.spacebase.mc.protocol.packet.ingame.server.ServerJoinGamePacket;
import ch.spacebase.mc.util.message.Message;
import ch.spacebase.packetlib.Client;
import ch.spacebase.packetlib.event.session.DisconnectedEvent;
import ch.spacebase.packetlib.event.session.PacketReceivedEvent;
import ch.spacebase.packetlib.event.session.SessionAdapter;
import ch.spacebase.packetlib.tcp.TcpSessionFactory;

public class MinecraftModule implements Module {

	private Bot bot;
	private Client conn;
	private String username;
	private List<ChatData> incoming = new ArrayList<ChatData>();
	
	public MinecraftModule(Bot bot, String host, int port, String username, String password) throws AuthenticationException {
		this.bot = bot;
		this.username = username;
		this.conn = new Client(host, port, new MinecraftProtocol(username, password), new TcpSessionFactory());
		this.conn.getSession().addListener(new BotListener());
	}
	
	@Override
	public void connect() {
		this.conn.getSession().connect();
	}

	@Override
	public void disconnect(String reason) {
		if(this.conn.getSession().isConnected()) {
			this.conn.getSession().disconnect(reason);
		}
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
		this.conn.getSession().send(new ClientChatPacket("[bot] " + message));
	}

	@Override
	public void update() {
	}
	
	private class BotListener extends SessionAdapter {
		
		@Override
		public void packetReceived(PacketReceivedEvent event) {
			if(event.getPacket() instanceof ServerJoinGamePacket) {
				chat(bot.getName() + " v" + bot.getVersion() + " connected.");
				chat("Using " + LibraryInfo.NAME + " v" + LibraryInfo.VERSION + ".");
			} else if(event.getPacket() instanceof ServerChatPacket) {
				this.parseChat(event.<ServerChatPacket>getPacket().getMessage());
			}
		}
		
		@Override
		public void disconnected(DisconnectedEvent event) {
			System.out.println(getMessagePrefix() + " Disconnected: " + new Message(event.getReason()).getRawText());
		}
		
		private void parseChat(Message message) {
			String user = null;
			String msg = null;
			if(message.getTranslate() == null || message.getTranslateWith() == null) {
				String text = message.getRawText();
				user = text.replaceAll("§[1-9a-z]", "").substring(0, text.indexOf(' '));
				msg = text.replaceAll("§[1-9a-z]", "").substring(text.indexOf(' '));
				// Parse common chat formatting
				user = user.replaceAll("<", "").replaceAll(">", "").replace(" [g]: ", "");
				if(msg.startsWith(": ")) {
					msg = msg.replaceFirst(": ", "");
				}
			} else {
				if(message.getTranslate().equals("chat.type.text")) {
					user = message.getTranslateWith()[0].getText();
					msg = message.getTranslateWith()[1].getText();
				}
			}
			
			if(user != null && msg != null) {
				incoming.add(new ChatData(user, msg));
			}
		}
	}

}
