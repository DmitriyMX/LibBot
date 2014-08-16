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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandManager {
	private Map<String, ExecutionInfo> commands = new HashMap<String, ExecutionInfo>();
	private CommandParser parser = new SpacedCommandParser();
	private PermissionManager permManager = new EmptyPermissionManager();
	private boolean multiThreaded = false;
	private String prefix = "#";
	private String unknownCommandFormat = "Unknown command \"%1$s\", %2$s.";
	private String noPermissionFormat = "You don't have permission to use \"%1$s\", %2$s.";
	private String incorrectUsageFormat = "Incorrect usage of \"%1$s\", %2$s.";
	private String usageFormat = "Usage: %1$s";

	private static final Pattern EXEC_PATTERN = Pattern.compile("\\!exec\\{([^}]+)\\}");

	public CommandParser getParser() {
		return this.parser;
	}

	public void setParser(CommandParser parser) {
		this.parser = parser;
	}

	public PermissionManager getPermissionManager() {
		return this.permManager;
	}

	public void setPermissionManager(PermissionManager manager) {
		this.permManager = manager;
	}

	public boolean isMultiThreaded() {
		return this.multiThreaded;
	}

	public void setMultiThreaded(boolean multiThreaded) {
		this.multiThreaded = multiThreaded;
	}

	public String getPrefix() {
		return this.prefix;
	}

	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	public String getUnknownCommandFormat() {
		return this.unknownCommandFormat;
	}

	public void setUnknownCommandFormat(String format) {
		this.unknownCommandFormat = format;
	}

	public String getNoPermissionFormat() {
		return this.noPermissionFormat;
	}

	public void setNoPermissionFormat(String format) {
		this.noPermissionFormat = format;
	}

	public String getIncorrectUsageFormat() {
		return this.incorrectUsageFormat;
	}

	public void setIncorrectUsageFormat(String format) {
		this.incorrectUsageFormat = format;
	}

	public String getUsageFormat() {
		return this.usageFormat;
	}

	public void setUsageFormat(String format) {
		this.usageFormat = format;
	}

	public void register(CommandExecutor exec) {
		for(Method method : exec.getClass().getDeclaredMethods()) {
			Command command = method.getAnnotation(Command.class);
			if(command != null) {
				for(String alias : command.aliases()) {
					this.commands.put(alias, new ExecutionInfo(exec, command, method));
				}
			}
		}
	}

	public void remove(CommandExecutor exec) {
		for(String command : this.commands.keySet()) {
			ExecutionInfo info = this.commands.get(command);
			if(info.getExecutor() == exec) {
				this.commands.remove(command);
			}
		}
	}

	public List<Command> getCommands() {
		List<Command> ret = new ArrayList<Command>();
		for(ExecutionInfo info : this.commands.values()) {
			if(!ret.contains(info.getCommand())) {
				ret.add(info.getCommand());
			}
		}

		return ret;
	}

	public List<Command> getCommands(Module source, String user) {
		List<Command> ret = new ArrayList<Command>();
		for(ExecutionInfo info : this.commands.values()) {
			if(!ret.contains(info.getCommand()) && this.permManager.hasPermission(source, user, info.getCommand().permission())) {
				ret.add(info.getCommand());
			}
		}

		return ret;
	}

	public void execute(final Module source, final ChatData message) {
		this.execute(source, message, false);
	}

	private void execute(final Module source, final ChatData message, boolean ignoreThreading) {
		String msg = message.getMessage().substring(this.prefix.length());
		final String cmd = this.parser.getCommand(msg).toLowerCase();
		String prefixed = this.prefix + cmd;
		String joined = join(this.parser.getArguments(msg));
		if(!joined.isEmpty()) {
			Matcher matcher = EXEC_PATTERN.matcher(joined);
			while(matcher.find()) {
				ArgsExecutor exec = new ArgsExecutor(source.getUsername());
				this.execute(exec, new ChatData(source.getUsername(), matcher.group(1)), true);
				joined = joined.replaceFirst(Pattern.quote(matcher.group(0)), exec.getResult());
			}
		}

		final String args[] = !joined.isEmpty() ? joined.split(" ") : new String[0];
		final ExecutionInfo exec = this.commands.get(cmd);
		if(exec == null) {
			if(this.unknownCommandFormat != null) {
				source.chat(String.format(this.unknownCommandFormat, prefixed, message.getUser()));
			}

			return;
		}

		Command command = exec.getCommand();
		if(!this.permManager.hasPermission(source, message.getUser(), command.permission())) {
			if(this.noPermissionFormat != null) {
				source.chat(String.format(this.noPermissionFormat, prefixed, message.getUser()));
			}

			return;
		}

		if(args.length < command.min() || (command.max() != -1 && args.length > command.max())) {
			StringBuilder build = new StringBuilder();
			if(this.incorrectUsageFormat != null) {
				build.append(String.format(this.incorrectUsageFormat, prefixed, message.getUser())).append("\n");
			}

			if(this.usageFormat != null) {
				build.append(String.format(this.usageFormat, prefixed + " " + command.usage())).append("\n");
			}

			String out = build.toString().trim();
			if(!out.isEmpty()) {
				source.chat(out);
			}

			return;
		}

		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				exec.execute(source, CommandManager.this, message.getUser(), cmd, args);
			}
		};

		if(this.multiThreaded && !ignoreThreading) {
			new Thread(runnable).start();
		} else {
			runnable.run();
		}
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
		private CommandExecutor executor;
		private Command cmd;
		private Method method;

		public ExecutionInfo(CommandExecutor executor, Command cmd, Method method) {
			this.method = method;
			this.cmd = cmd;
			this.executor = executor;
		}

		public CommandExecutor getExecutor() {
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
