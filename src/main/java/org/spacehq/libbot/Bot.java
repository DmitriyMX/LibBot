package org.spacehq.libbot;

import org.spacehq.libbot.chat.ChatData;
import org.spacehq.libbot.chat.cmd.CommandManager;
import org.spacehq.libbot.module.Module;

import java.util.*;

public abstract class Bot {
	private boolean running = true;
	private boolean acceptSelfCommands;
	private final CommandManager commands = new CommandManager();
	private final Map<String, Module> modules = new LinkedHashMap<String, Module>();
	private final Map<String, Module> newModules = new LinkedHashMap<String, Module>();

	public final void start(String args[], boolean acceptSelfCommands) {
		this.acceptSelfCommands = acceptSelfCommands;
		this.initBot(args);
		this.run();
	}

	public abstract void initBot(String args[]);

	public abstract void shutdownBot();

	public abstract void onChat(Module module, ChatData data);

	public abstract boolean onCommand(Module module, ChatData data);

	private final void run() {
		while(this.running) {
			for(String id : this.newModules.keySet()) {
				Module module = this.newModules.get(id);
				if(this.modules.containsKey(id)) {
					this.removeModule(id);
				}

				System.out.println(module.getMessagePrefix() + " Connecting module...");
				if(module.getUsername() == null || module.getUsername().isEmpty()) {
					module.setUsername("Bot");
				}

				try {
					module.connect();
					this.modules.put(id, module);
					System.out.println(module.getMessagePrefix() + " Module connected.");
				} catch(Exception e) {
					System.err.println(module.getMessagePrefix() + " An error occured while connecting the module.");
					e.printStackTrace();
				}
			}

			this.newModules.clear();
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
		this.shutdownBot();
		for(String id : this.modules.keySet()) {
			Module module = this.getModule(id);
			System.out.println(module.getMessagePrefix() + " Disconnecting module...");
			try {
				module.disconnect("Bot shutting down.");
				System.out.println(module.getMessagePrefix() + " Module disconnected.");
			} catch(Exception e) {
				System.err.println(module.getMessagePrefix() + " An error occured while disconnecting the module.");
				e.printStackTrace();
			}

			this.removeModule(id);
		}
	}

	public final boolean isRunning() {
		return this.running;
	}

	public final void stop() {
		this.running = false;
	}

	public final CommandManager getCommandManager() {
		return this.commands;
	}

	public final List<Module> getModules() {
		return new ArrayList<Module>(this.modules.values());
	}

	public final Module getModule(String id) {
		return this.modules.get(id);
	}

	public final Module addModule(String id, Module module) {
		this.newModules.put(id, module);
		return module;
	}

	public final Module removeModule(String id) {
		return this.modules.remove(id);
	}
}
