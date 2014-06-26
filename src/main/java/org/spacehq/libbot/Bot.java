package org.spacehq.libbot;

import org.spacehq.libbot.chat.ChatData;
import org.spacehq.libbot.chat.cmd.CommandManager;
import org.spacehq.libbot.module.Module;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class Bot {
	private boolean running = true;
	private String name;
	private String version;
	private boolean acceptSelfCommands;
	private final CommandManager commands = new CommandManager();
	private final Map<Class<? extends Module>, Module> modules = new LinkedHashMap<Class<? extends Module>, Module>();

	public final void start(String name, String version, boolean acceptSelfCommands, String args[]) {
		this.name = name;
		this.version = version;
		this.acceptSelfCommands = acceptSelfCommands;
		System.out.println("Starting " + this.name + " v" + this.version + "...");
		System.out.println("Using " + LibraryInfo.NAME + " v" + LibraryInfo.VERSION);
		this.initBot(args);
		for(Module module : this.modules.values()) {
			System.out.println(module.getMessagePrefix() + " Connecting module...");
			if(module.getUsername() == null || module.getUsername().isEmpty()) {
				module.setUsername(this.name);
			}

			try {
				module.connect();
				System.out.println(module.getMessagePrefix() + " Module connected.");
			} catch(Exception e) {
				System.err.println(module.getMessagePrefix() + " An error occured while connecting the module.");
				e.printStackTrace();
				this.modules.remove(module.getClass());
			}
		}

		this.run();
	}

	public abstract void initBot(String args[]);

	public abstract void onChat(Module module, ChatData data);

	public abstract boolean onCommand(Module module, ChatData data);

	private final void run() {
		while(this.running) {
			for(Module module : this.modules.values()) {
				try {
					module.update();
					List<ChatData> chat = module.getIncomingChat();
					for(ChatData data : chat) {
						if(data != null) {
							System.out.println(module.getMessagePrefix() + " " + data.getUser() + ": " + data.getMessage());
							if(data.getMessage().startsWith(this.commands.getPrefix())) {
								if(this.acceptSelfCommands || !data.getUser().equals(module.getUsername())) {
									if(this.onCommand(module, data)) {
										try {
											this.commands.execute(module, data);
										} catch(Exception e) {
											System.err.println(module.getMessagePrefix() + " An error occured while executing a command.");
											e.printStackTrace();
										}
									}
								}
							} else {
								this.onChat(module, data);
							}
						}
					}
				} catch(Exception e) {
					System.err.println(module.getMessagePrefix() + " An error occured while updating the module.");
					e.printStackTrace();
				}
			}
		}

		this.shutdown();
	}

	private final void shutdown() {
		this.running = false;
		for(Module module : this.modules.values()) {
			System.out.println(module.getMessagePrefix() + " Disconnecting module...");
			try {
				module.disconnect("Bot shutting down.");
				System.out.println(module.getMessagePrefix() + " Module disconnected.");
			} catch(Exception e) {
				System.err.println(module.getMessagePrefix() + " An error occured while disconnecting the module.");
				e.printStackTrace();
			}

			this.modules.remove(module.getClass());
		}
	}

	public final boolean isRunning() {
		return this.running;
	}

	public final void stop() {
		this.running = false;
	}

	public final String getName() {
		return this.name;
	}

	public final String getVersion() {
		return this.version;
	}

	public final CommandManager getCommandManager() {
		return this.commands;
	}

	public final Module addModule(Module module) {
		return this.modules.put(module.getClass(), module);
	}

	public final Module getModule(Class<? extends Module> clazz) {
		return this.modules.get(clazz);
	}

	public final Module removeModule(Class<? extends Module> clazz) {
		return this.modules.remove(clazz);
	}
}
