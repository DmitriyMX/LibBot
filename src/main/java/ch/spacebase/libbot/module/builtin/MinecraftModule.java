package ch.spacebase.libbot.module.builtin;

import java.util.ArrayList;
import java.util.List;

import ch.spacebase.libbot.Bot;
import ch.spacebase.libbot.LibraryInfo;
import ch.spacebase.libbot.chat.ChatData;
import ch.spacebase.libbot.module.Module;
import ch.spacebase.mcprotocol.event.PacketRecieveEvent;
import ch.spacebase.mcprotocol.event.ProtocolListener;
import ch.spacebase.mcprotocol.exception.ConnectException;
import ch.spacebase.mcprotocol.exception.LoginException;
import ch.spacebase.mcprotocol.exception.OutdatedLibraryException;
import ch.spacebase.mcprotocol.net.Client;
import ch.spacebase.mcprotocol.standard.StandardProtocol;
import ch.spacebase.mcprotocol.standard.packet.PacketChat;
import ch.spacebase.mcprotocol.standard.packet.PacketDisconnect;


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
		this.conn = new Client(new StandardProtocol(), host, port);
		this.conn.listen(new BotListener());
	}
	
	@Override
	public void connect() {
		try {
			this.conn.setUser(this.username, this.password);
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
		private static final int DISCONNECT = 255;
		
		@Override
		public void onPacketRecieve(PacketRecieveEvent event) {
			if(event.getPacket().getId() == LOGIN) {
				chat(bot.getName() + " v" + bot.getVersion() + " connected.");
				chat("Using " + LibraryInfo.NAME + " v" + LibraryInfo.VERSION + ".");
			} else if(event.getPacket().getId() == CHAT) {
				this.parseChat(event.getPacket(PacketChat.class).getMessage());
			} else if(event.getPacket().getId() == DISCONNECT) {
				disconnect(event.getPacket(PacketDisconnect.class).getReason());
			}
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
