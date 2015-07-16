package org.spacehq.libbot.module.builtin;

import org.spacehq.libbot.Bot;
import org.spacehq.libbot.chat.ChatData;
import org.spacehq.libbot.module.Module;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class ConsoleModule implements Module {
	private List<ChatData> incoming = new ArrayList<ChatData>();
	private ConsoleReader reader = new ConsoleReader();
	private Bot bot;

	public ConsoleModule(Bot bot) {
		if(bot == null) {
			throw new IllegalArgumentException("Bot cannot be null.");
		}

		this.bot = bot;
	}

	@Override
	public void connect() {
		new Thread(this.reader, "ConsoleReader").start();
	}

	@Override
	public void disconnect(String reason) {
		this.reader.stopReading();
	}

	@Override
	public String getUsername() {
		return "Console";
	}

	@Override
	public void setUsername(String name) {
		throw new UnsupportedOperationException("ConsoleModule does not have a username.");
	}

	@Override
	public String getMessagePrefix() {
		return "[" + this.getUsername() + "]";
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

	private class ConsoleReader implements Runnable {
		private boolean reading = true;

		public void stopReading() {
			this.reading = false;
		}

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
					} else {
						try {
							Thread.sleep(100);
						} catch(InterruptedException e) {
						}
					}
				} catch(IOException e) {
					System.err.println("Failed to read line from console!");
					e.printStackTrace();
				}
			}
		}
	}
}
