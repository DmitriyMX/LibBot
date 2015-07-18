package org.spacehq.libbot;

import org.spacehq.libbot.chat.ChatData;
import org.spacehq.libbot.chat.cmd.CommandManager;
import org.spacehq.libbot.module.Module;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * The main class of a bot.
 */
public abstract class Bot {
    private boolean running = true;
    private final CommandManager commands = new CommandManager();
    private final Map<String, Module> modules = new HashMap<String, Module>();

    /**
     * Starts the bot.
     *
     * @param args Arguments to pass to the bot.
     */
    public final void start(String args[]) {
        try {
            this.initBot(args);
        } catch(Throwable t) {
            System.err.println("[Bot] An error occurred while initializing the bot.");
            t.printStackTrace();
            return;
        }

        this.run();
    }

    /**
     * Called when initializing the bot.
     *
     * @param args Arguments passed to the bot.
     */
    public abstract void initBot(String args[]);

    /**
     * Called when shutting down the bot.
     */
    public abstract void shutdownBot();

    /**
     * Called when a chat message is received.
     *
     * @param module Module that the chat message originated from.
     * @param data   Details of the received chat message.
     */
    public abstract void onChat(Module module, ChatData data);

    /**
     * Called when a chat message containing a command is received.
     *
     * @param module Module that the chat message originated from.
     * @param data   Details of the received chat message.
     * @return Whether the command should be executed.
     */
    public abstract boolean onCommand(Module module, ChatData data);

    private final void run() {
        while(this.running) {
            try {
                for(Module module : this.modules.values()) {
                    if(module.isConnected()) {
                        try {
                            List<ChatData> chat = module.getIncomingChat();
                            for(ChatData data : chat) {
                                if(data != null) {
                                    System.out.println("[" + module.getId() + "] " + data.getUser() + ": " + data.getMessage());
                                    if(data.getMessage().startsWith(this.commands.getPrefix()) && (this.commands.getAcceptCommandsFromSelf() || !data.getUser().equals(module.getUsername()))) {
                                        boolean execute = false;
                                        try {
                                            execute = this.onCommand(module, data);
                                        } catch(Throwable t) {
                                            System.err.println("[" + module.getId() + "] An error occurred while handling a command.");
                                            t.printStackTrace();
                                        }

                                        if(execute) {
                                            try {
                                                this.commands.execute(module, data);
                                            } catch(Exception e) {
                                                System.err.println("[" + module.getId() + "] An error occurred while executing a command.");
                                                e.printStackTrace();
                                            }
                                        }
                                    } else {
                                        try {
                                            this.onChat(module, data);
                                        } catch(Throwable t) {
                                            System.err.println("[" + module.getId() + "] An error occurred while handling chat.");
                                            t.printStackTrace();
                                        }
                                    }
                                }
                            }
                        } catch(Throwable t) {
                            System.err.println("[" + module.getId() + "] An error occurred while handling chat.");
                            t.printStackTrace();
                        }
                    } else {
                        this.removeModule(module);
                    }
                }
            } catch(Throwable t) {
                System.err.println("[Bot] An error occurred while updating modules.");
                t.printStackTrace();
            }

            try {
                Thread.sleep(100);
            } catch(InterruptedException e) {
                break;
            }
        }

        try {
            this.shutdown();
        } catch(Throwable t) {
            System.err.println("[Bot] An error occurred while shutting down the bot.");
            t.printStackTrace();
        }
    }

    private final void shutdown() {
        this.running = false;
        try {
            this.shutdownBot();
        } catch(Throwable t) {
            System.err.println("[Bot] An error occurred while handling bot shutdown.");
            t.printStackTrace();
        }

        for(String id : new HashSet<String>(this.modules.keySet())) {
            this.removeModule(id);
        }
    }

    /**
     * Gets whether the bot is currently running.
     *
     * @return Whether the bot is currently running.
     */
    public final boolean isRunning() {
        return this.running;
    }

    /**
     * Stops the bot, halting execution.
     */
    public final void stop() {
        this.running = false;
    }

    /**
     * Gets the bot's command manager.
     *
     * @return The bot's command manager.
     */
    public final CommandManager getCommandManager() {
        return this.commands;
    }

    /**
     * Gets a list of modules registered to the bot.
     *
     * @return A list of modules registered to the bot.
     */
    public final Collection<Module> getModules() {
        return this.modules.values();
    }

    /**
     * Gets the module with the given ID.
     *
     * @param id ID of the module.
     * @return The module with the given ID.
     */
    public final Module getModule(String id) {
        return this.modules.get(id);
    }

    /**
     * Adds a module to the bot.
     *
     * @param module Module to add.
     * @return The added module, or null if adding the module failed.
     */
    public final Module addModule(Module module) {
        if(module == null) {
            return null;
        }

        if(this.modules.containsKey(module.getId())) {
            this.removeModule(module.getId());
        }

        try {
            System.out.println("[" + module.getId() + "] Connecting module...");
            module.connect();
            this.modules.put(module.getId(), module);
            System.out.println("[" + module.getId() + "] Module connected.");

            return module;
        } catch(Throwable t) {
            System.err.println("[" + module.getId() + "] An error occurred while connecting the module.");
            t.printStackTrace();

            return null;
        }
    }

    /**
     * Removes a module from the bot.
     *
     * @param module Module to remove.
     * @return The removed module.
     */
    public final Module removeModule(Module module) {
        if(module == null) {
            return null;
        }

        return this.removeModule(module.getId());
    }

    /**
     * Removes a module from the bot.
     *
     * @param id ID of the module to remove.
     * @return The removed module.
     */
    public final Module removeModule(String id) {
        if(id == null || !this.modules.containsKey(id)) {
            return null;
        }

        Module module = this.modules.get(id);
        if(module.isConnected()) {
            try {
                System.out.println("[" + module.getId() + "] Disconnecting module...");
                module.disconnect("Module removed.");
                System.out.println("[" + module.getId() + "] Module disconnected.");
            } catch(Throwable t) {
                System.err.println("[" + module.getId() + "] An error occurred while disconnecting the module.");
                t.printStackTrace();
            }
        }

        return this.modules.remove(id);
    }
}
