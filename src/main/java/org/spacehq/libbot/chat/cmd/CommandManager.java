package org.spacehq.libbot.chat.cmd;

import org.spacehq.libbot.chat.ChatData;
import org.spacehq.libbot.chat.cmd.parser.CommandParser;
import org.spacehq.libbot.chat.cmd.parser.SpacedCommandParser;
import org.spacehq.libbot.chat.cmd.permission.EmptyPermissionManager;
import org.spacehq.libbot.chat.cmd.permission.PermissionManager;
import org.spacehq.libbot.module.Module;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages commands and their basic configuration.
 */
public class CommandManager {
    private Map<String, ExecutionInfo> commands = new HashMap<String, ExecutionInfo>();
    private CommandParser parser = new SpacedCommandParser();
    private PermissionManager permManager = new EmptyPermissionManager();
    private boolean multiThreaded = false;
    private boolean acceptCommandsFromSelf = false;
    private String prefix = "#";
    private String unknownCommandFormat = "Unknown command \"%1$s\", %2$s.";
    private String permissionDeniedFormat = "You don't have permission to use \"%1$s\", %2$s.";
    private String incorrectUsageFormat = "Incorrect usage of \"%1$s\", %2$s.";
    private String usageFormat = "Usage: %1$s";

    /**
     * Gets the current parser used for parsing commands from a chat message.
     *
     * @return The current command parser.
     */
    public CommandParser getParser() {
        return this.parser;
    }

    /**
     * Sets the current parser used for parsing commands from a chat message.
     *
     * @param parser Parser to use.
     */
    public void setParser(CommandParser parser) {
        this.parser = parser;
    }

    /**
     * Gets the current permission manager.
     *
     * @return The current permission manager.
     */
    public PermissionManager getPermissionManager() {
        return this.permManager;
    }

    /**
     * Sets the current permission manager.
     *
     * @param manager Permission manager to use.
     */
    public void setPermissionManager(PermissionManager manager) {
        this.permManager = manager;
    }

    /**
     * Gets whether commands should be executed in a unique thread.
     *
     * @return Whether commands should be multithreaded.
     */
    public boolean isMultiThreaded() {
        return this.multiThreaded;
    }

    /**
     * Sets whether commands should be executed in a unique thread.
     *
     * @return Whether commands should be multithreaded.
     */
    public void setMultiThreaded(boolean multiThreaded) {
        this.multiThreaded = multiThreaded;
    }

    /**
     * Gets whether commands sent using the bot's username should be accepted.
     *
     * @return Whether commands sent using the bot's username should be accepted.
     */
    public boolean getAcceptCommandsFromSelf() {
        return this.acceptCommandsFromSelf;
    }

    /**
     * Sets whether commands sent using the bot's username should be accepted.
     *
     * @return Whether commands sent using the bot's username should be accepted.
     */
    public void setAcceptCommandsFromSelf(boolean accept) {
        this.acceptCommandsFromSelf = accept;
    }

    /**
     * Gets the prefix used to signify a command.
     *
     * @return The current command prefix.
     */
    public String getPrefix() {
        return this.prefix;
    }

    /**
     * Sets the prefix used to signify a command.
     *
     * @param prefix Command prefix to use.
     */
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    /**
     * Gets the format used when sending an unknown command message.
     * The first parameter provided is the command name, and the second parameter provided is the user who used the command.
     *
     * @return The format used when sending an unknown command message.
     */
    public String getUnknownCommandFormat() {
        return this.unknownCommandFormat;
    }

    /**
     * Sets the format used when sending an unknown command message.
     * The first parameter provided is the command name, and the second parameter provided is the user who used the command.
     *
     * @return Format to use when sending an unknown command message.
     */
    public void setUnknownCommandFormat(String format) {
        this.unknownCommandFormat = format;
    }

    /**
     * Gets the format used when sending a permission denied message.
     * The first parameter provided is the command name the permission is for, and the second parameter provided is the user who used the command.
     *
     * @return The format used when sending a permission denied message.
     */
    public String getPermissionDeniedFormat() {
        return this.permissionDeniedFormat;
    }

    /**
     * Sets the format used when sending a permission denied message.
     * The first parameter provided is the command name the permission is for, and the second parameter provided is the user who used the command.
     *
     * @return Format to use when sending a permission denied message.
     */
    public void setPermissionDeniedFormat(String format) {
        this.permissionDeniedFormat = format;
    }

    /**
     * Gets the format used when sending an incorrect usage message.
     * The first parameter provided is the command name, and the second parameter provided is the user who used the command.
     *
     * @return The format used when sending an incorrect usage message.
     */
    public String getIncorrectUsageFormat() {
        return this.incorrectUsageFormat;
    }

    /**
     * Sets the format used when sending an incorrect usage message.
     * The first parameter provided is the command name, and the second parameter provided is the user who used the command.
     *
     * @return Format to use when sending an incorrect usage message.
     */
    public void setIncorrectUsageFormat(String format) {
        this.incorrectUsageFormat = format;
    }

    /**
     * Gets the format used when sending a usage message.
     * The only parameter provided is the command's usage information.
     *
     * @return The format used when sending a usage message.
     */
    public String getUsageFormat() {
        return this.usageFormat;
    }

    /**
     * Sets the format used when sending a usage message.
     * The only parameter provided is the command's usage information.
     *
     * @return Format to use when sending a usage message.
     */
    public void setUsageFormat(String format) {
        this.usageFormat = format;
    }

    /**
     * Registers a command executor.
     * Command executors should contain methods with a command annotation and the following arguments:
     * (Module source, CommandManager commands, String sender, String alias, String args[])
     *
     * @param executor Executor to register.
     */
    public void register(Object executor) {
        for(Method method : executor.getClass().getDeclaredMethods()) {
            Command command = method.getAnnotation(Command.class);
            if(command != null) {
                for(String alias : command.aliases()) {
                    this.commands.put(alias, new ExecutionInfo(executor, command, method));
                }
            }
        }
    }

    /**
     * Unregisters a command executor.
     *
     * @param executor Executor to unregister.
     */
    public void unregister(Object executor) {
        for(String command : this.commands.keySet()) {
            ExecutionInfo info = this.commands.get(command);
            if(info.getExecutor() == executor) {
                this.commands.remove(command);
            }
        }
    }

    /**
     * Gets a list of registered commands.
     *
     * @return A list of registered commands.
     */
    public List<Command> getCommands() {
        List<Command> ret = new ArrayList<Command>();
        for(ExecutionInfo info : this.commands.values()) {
            if(!ret.contains(info.getCommand())) {
                ret.add(info.getCommand());
            }
        }

        return ret;
    }

    /**
     * Gets a list of commands available to the given user from the given source.
     *
     * @param source Module that the user is communicating from.
     * @param user   User to get commands for.
     * @return As list of ommands available to the given user from the given source.
     */
    public List<Command> getCommands(Module source, String user) {
        List<Command> ret = new ArrayList<Command>();
        for(ExecutionInfo info : this.commands.values()) {
            if(!ret.contains(info.getCommand()) && this.permManager.hasPermission(source, user, info.getCommand().permission())) {
                ret.add(info.getCommand());
            }
        }

        return ret;
    }

    /**
     * Attempts to execute a command from the given message.
     *
     * @param source  Module that the user is communicating from.
     * @param message Chat message containing the command.
     */
    public void execute(Module source, ChatData message) {
        this.execute(source, message, false);
    }

    private void execute(final Module source, final ChatData message, boolean ignoreThreading) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                String msg = message.getMessage().substring(prefix.length());
                String cmd = parser.getCommand(msg).toLowerCase();
                if(cmd.isEmpty()) {
                    return;
                }

                String prefixed = prefix + cmd;
                String args[] = parseArgs(source, join(parser.getArguments(msg)));
                ExecutionInfo exec = commands.get(cmd);
                if(exec == null) {
                    if(unknownCommandFormat != null) {
                        source.chat(String.format(unknownCommandFormat, prefixed, message.getUser()));
                    }

                    return;
                }

                Command command = exec.getCommand();
                if(!permManager.hasPermission(source, message.getUser(), command.permission())) {
                    if(permissionDeniedFormat != null) {
                        source.chat(String.format(permissionDeniedFormat, prefixed, message.getUser()));
                    }

                    return;
                }

                if(args.length < command.min() || (command.max() != -1 && args.length > command.max())) {
                    StringBuilder build = new StringBuilder();
                    if(incorrectUsageFormat != null) {
                        build.append(String.format(incorrectUsageFormat, prefixed, message.getUser())).append("\n");
                    }

                    if(usageFormat != null) {
                        build.append(String.format(usageFormat, prefixed + " " + command.usage())).append("\n");
                    }

                    String out = build.toString().trim();
                    if(!out.isEmpty()) {
                        source.chat(out);
                    }

                    return;
                }

                exec.execute(source, CommandManager.this, message.getUser(), cmd, args);
            }
        };

        if(this.multiThreaded && !ignoreThreading) {
            new Thread(runnable).start();
        } else {
            runnable.run();
        }
    }

    private String[] parseArgs(Module source, String str) {
        int execStart = -1;
        int execCount = 0;
        int argStart = -1;
        List<String> args = new ArrayList<String>();
        StringBuilder arg = new StringBuilder();
        for(int index = 0; index < str.length(); index++) {
            char c = str.charAt(index);
            if(c == '!' && str.indexOf("!exec{", index) == index) {
                if(execStart == -1) {
                    execStart = index + "!exec{".length();
                }

                execCount++;
            } else if(execStart != -1) {
                if(c == '}' && str.charAt(index - 1) != '\\') {
                    execCount--;
                    if(execCount == 0 && index - execStart > 0) {
                        ArgsExecutor exec = new ArgsExecutor(source);
                        String cmd = str.substring(execStart, index);
                        if(!cmd.startsWith(this.getPrefix())) {
                            cmd = this.getPrefix() + cmd;
                        }

                        this.execute(exec, new ChatData(source.getUsername(), cmd), true);
                        arg.append(exec.getResult());
                        execStart = -1;
                    }
                }
            } else {
                if(c != ' ') {
                    arg.append(c);
                } else if(arg.length() > 0) {
                    args.add(arg.toString());
                    arg = new StringBuilder();
                }
            }
        }

        if(arg.length() > 0) {
            args.add(arg.toString());
        }

        return args.toArray(new String[args.size()]);
    }

    private static String join(String strs[]) {
        StringBuilder build = new StringBuilder();
        boolean first = true;
        for(String str : strs) {
            if(!first) {
                build.append(" ");
            }

            first = false;
            build.append(str);
        }

        return build.toString();
    }

    private static class ExecutionInfo {
        private Object executor;
        private Command cmd;
        private Method method;

        public ExecutionInfo(Object executor, Command cmd, Method method) {
            this.method = method;
            this.cmd = cmd;
            this.executor = executor;
        }

        public Object getExecutor() {
            return this.executor;
        }

        public Command getCommand() {
            return this.cmd;
        }

        public void execute(Module source, CommandManager commands, String sender, String alias, String args[]) {
            try {
                this.method.invoke(this.executor, source, commands, sender, alias, args);
            } catch(Exception e) {
                System.err.println("Error executing command.");
                e.printStackTrace();
            }
        }
    }
}
