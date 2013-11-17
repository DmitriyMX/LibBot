package ch.spacebase.libbot.module.builtin;

import java.util.ArrayList;
import java.util.List;

import ch.spacebase.libbot.Bot;
import ch.spacebase.libbot.LibraryInfo;
import ch.spacebase.libbot.chat.ChatData;
import ch.spacebase.libbot.module.Module;
import ch.spacebase.mcprotocol.event.DisconnectEvent;
import ch.spacebase.mcprotocol.event.PacketReceiveEvent;
import ch.spacebase.mcprotocol.event.PacketSendEvent;
import ch.spacebase.mcprotocol.event.ProtocolListener;
import ch.spacebase.mcprotocol.exception.ConnectException;
import ch.spacebase.mcprotocol.exception.LoginException;
import ch.spacebase.mcprotocol.exception.OutdatedLibraryException;
import ch.spacebase.mcprotocol.net.Client;
import ch.spacebase.mcprotocol.standard.StandardClient;
import ch.spacebase.mcprotocol.standard.packet.PacketChat;

public class MinecraftModule implements Module {

	private Bot bot;
	private Client conn;
	private String username;
	private String password;
	private List<ChatData> incoming = new ArrayList<ChatData>();
	
	public MinecraftModule(Bot bot, String host, int port, String username, String password) {
		this.bot = bot;
		this.username = username;
		this.password = password;
		this.conn = new StandardClient(host, port);
		this.conn.listen(new BotListener());
	}
	
	@Override
	public void connect() {
		try {
			this.conn.login(this.username, this.password);
		} catch (LoginException e) {
			e.printStackTrace();
			return;
		} catch (OutdatedLibraryException e) {
			e.printStackTrace();
			return;
		}
		
		try {
			this.conn.connect();
		} catch (ConnectException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void disconnect(String reason) {
		System.out.println(this.getMessagePrefix() + " Disconnected: " + reason);
		if(this.conn.isConnected()) this.conn.disconnect(reason);
	}

	@Override
	public String getUsername() {
		return this.username;
	}
	
	@Override
	public void setUsername(String name) {
		System.err.println("[NOTICE] Cannot set name using MinecraftModule.");
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
		this.conn.send(new PacketChat("[bot] " + message));
	}

	@Override
	public void update() {
	}
	
	private class BotListener extends ProtocolListener {
		private static final int LOGIN = 1;
		private static final int CHAT = 3;
		
		@Override
		public void onPacketReceive(PacketReceiveEvent event) {
			if(event.getPacket().getId() == LOGIN) {
				chat(bot.getName() + " v" + bot.getVersion() + " connected.");
				chat("Using " + LibraryInfo.NAME + " v" + LibraryInfo.VERSION + ".");
			} else if(event.getPacket().getId() == CHAT) {
				this.parseChat(event.getPacket(PacketChat.class).getMessage());
			}
		}
		
		@Override
		public void onPacketSend(PacketSendEvent event) {
		}
		
		@Override
		public void onDisconnect(DisconnectEvent event) {
			disconnect(event.getReason());
		}
		
		private void parseChat(String msg) {
			String user = msg.replaceAll("§[1-9a-z]", "").substring(0, msg.indexOf(' '));
			String message = msg.replaceAll("§[1-9a-z]", "").substring(msg.indexOf(' '));
			// Parse common chat formatting
			user = user.replaceAll("<", "").replaceAll(">", "").replace(" [g]: ", "");
			if(message.startsWith(": ")) message = message.replaceFirst(": ", "");
			incoming.add(new ChatData(user, message));
		}
	}

}
