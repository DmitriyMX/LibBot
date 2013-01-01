package ch.spacebase.libbot.module.builtin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import ch.spacebase.libbot.Bot;
import ch.spacebase.libbot.chat.ChatData;
import ch.spacebase.libbot.module.Module;


public class ConsoleModule implements Module {

	private List<ChatData> incoming = new ArrayList<ChatData>();
	private ConsoleReader reader = new ConsoleReader();
	private Bot bot;
	
	public ConsoleModule(Bot bot) {
		this.bot = bot;
	}
	
	@Override
	public void connect() {
		this.reader.start();
	}

	@Override
	public void disconnect(String reason) {
		this.reader.stopReading();
	}

	@Override
	public String getUsername() {
		return "ConsoleListener";
	}

	@Override
	public void setUsername(String name) {
	}

	@Override
	public String getMessagePrefix() {
		return "[Console]";
	}

	@Override
	public List<ChatData> getIncomingChat() {
		List<ChatData> ret = new ArrayList<ChatData>(this.incoming);
		this.incoming.removeAll(ret);
		return ret;
	}

	@Override
	public void chat(String message) {
		System.out.println(message);
	}

	@Override
	public void update() {
	}
	
	private class ConsoleReader extends Thread {
		private boolean reading = true;
		
		@Override
		public void run() {
			BufferedReader read = new BufferedReader(new InputStreamReader(System.in));
			while(this.reading) {
				try {
					if(read.ready()) {
						String line = read.readLine();
						if(line.startsWith(bot.getCommandManager().getPrefix())) {
							incoming.add(new ChatData("Console", line));
						} else {
							incoming.add(new ChatData("Console", bot.getCommandManager().getPrefix() + line));
						}
					}
				} catch(IOException e) {
					System.err.println("Failed to read line from console!");
					e.printStackTrace();
				}
			}
		}

		public void stopReading() {
			this.reading = false;
		}
	}

}
