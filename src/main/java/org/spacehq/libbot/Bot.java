package org.spacehq.libbot;

import org.spacehq.libbot.chat.ChatData;
import org.spacehq.libbot.chat.cmd.CommandManager;
import org.spacehq.libbot.module.Module;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class Bot {
	private boolean running = true;
	private final CommandManager commands = new CommandManager();
	private final Map<String, Module> modules = new LinkedHashMap<String, Module>();

	public final void start(String args[]) {
		try {
			this.initBot(args);
		} catch(Throwable t) {
			System.err.println("[Bot] An error occured while initializing the bot.");
			t.printStackTrace();
			return;
		}

		this.run();
	}

	public abstract void initBot(String args[]);

	public abstract void shutdownBot();

	public abstract void onChat(String moduleId, Module module, ChatData data);

	public abstract boolean onCommand(String moduleId, Module module, ChatData data);

	private final void run() {
		while(this.running) {
			try {
				for(String id : this.modules.keySet()) {
					Module module = this.modules.get(id);
					if(module.isConnected()) {
						try {
							module.update();
							List<ChatData> chat = module.getIncomingChat();
							for(ChatData data : chat) {
								if(data != null) {
									System.out.println(module.getMessagePrefix() + " " + data.getUser() + ": " + data.getMessage());
									if(data.getMessage().startsWith(this.commands.getPrefix()) && (this.commands.getAcceptCommandsFromSelf() || !data.getUser().equals(module.getUsername()))) {
										boolean execute = false;
										try {
											execute = this.onCommand(id, module, data);
										} catch(Throwable t) {
											System.err.println(module.getMessagePrefix() + " An error occured while handling a command.");
											t.printStackTrace();
										}

										if(execute) {
											try {
												this.commands.execute(module, data);
											} catch(Exception e) {
												System.err.println(module.getMessagePrefix() + " An error occured while executing a command.");
												e.printStackTrace();
											}
										}
									} else {
										try {
											this.onChat(id, module, data);
										} catch(Throwable t) {
											System.err.println(module.getMessagePrefix() + " An error occured while handling chat.");
											t.printStackTrace();
										}
									}
								}
							}
						} catch(Throwable t) {
							System.err.println(module.getMessagePrefix() + " An error occured while updating the module.");
							t.printStackTrace();
						}
					} else {
						this.removeModule(id);
					}
				}
			} catch(Throwable t) {
				System.err.println("[Bot] An error occured while updating modules.");
				t.printStackTrace();
			}
		}

		try {
			this.shutdown();
		} catch(Throwable t) {
			System.err.println("[Bot] An error occured while shutting down the bot.");
			t.printStackTrace();
		}
	}

	private final void shutdown() {
		this.running = false;
		try {
			this.shutdownBot();
		} catch(Throwable t) {
			System.err.println("[Bot] An error occured while handling bot shutdown.");
			t.printStackTrace();
		}

		for(String id : this.modules.keySet()) {
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

	public final Collection<Module> getModules() {
		return this.modules.values();
	}

	public final Module getModule(String id) {
		return this.modules.get(id);
	}

	public final Module addModule(String id, Module module) {
		if(this.modules.containsKey(id)) {
			this.removeModule(id);
		}

		try {
			System.out.println(module.getMessagePrefix() + " Connecting module...");
			module.connect();
			this.modules.put(id, module);
			System.out.println(module.getMessagePrefix() + " Module connected.");

			return module;
		} catch(Throwable t) {
			System.err.println(module.getMessagePrefix() + " An error occured while connecting the module.");
			t.printStackTrace();

			return null;
		}
	}

	public final Module removeModule(String id) {
		if(!this.modules.containsKey(id)) {
			return null;
		}

		Module module = this.modules.get(id);
		if(module.isConnected()) {
			try {
				System.out.println(module.getMessagePrefix() + " Disconnecting module...");
				module.disconnect("Module removed.");
				System.out.println(module.getMessagePrefix() + " Module disconnected.");
			} catch(Throwable t) {
				System.err.println(module.getMessagePrefix() + " An error occured while disconnecting the module.");
				t.printStackTrace();
			}
		}

		return this.modules.remove(id);
	}
}
