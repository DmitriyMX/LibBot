package org.spacehq.libbot.chat.cmd;

import org.spacehq.libbot.chat.ChatData;
import org.spacehq.libbot.chat.cmd.parser.CommandParser;
import org.spacehq.libbot.chat.cmd.parser.SpacedCommandParser;
import org.spacehq.libbot.chat.cmd.permission.EmptyPermissionManager;
import org.spacehq.libbot.chat.cmd.permission.PermissionManager;
import org.spacehq.libbot.module.Module;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;


public class CommandManager {

	private List<CommandExecutor> executors = new ArrayList<CommandExecutor>();
	private CommandParser parser = new SpacedCommandParser();
	private PermissionManager permManager = new EmptyPermissionManager();
	private String prefix = "#";
	private String unknownCommandFormat = "Unknown command \"%1$s\", %2$s.";
	private String noPermissionFormat = "You don't have permission to use \"%1$s\", %2$s.";
	private String incorrectUsageFormat = "Incorrect usage of \"%1$s\", %2$s.";
	private String usageFormat = "Usage: %1$s";

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
		this.executors.add(exec);
	}

	public void remove(CommandExecutor exec) {
		this.executors.remove(exec);
	}

	public void execute(Module source, ChatData message) {
		String msg = message.getMessage().substring(this.prefix.length());
		String cmd = this.parser.getCommand(msg);
		String prefixed = this.prefix + cmd;
		String args[] = this.parser.getArguments(msg);
		ExecutionInfo exec = this.getCommand(cmd);
		if(exec == null) {
			if(this.unknownCommandFormat != null) {
				source.chat(String.format(this.unknownCommandFormat, prefixed, message.getUser()));
			}

			return;
		}

		Command command = exec.getCommand();
		if(!this.permManager.hasPermission(message.getUser(), command.permission())) {
			if(this.noPermissionFormat != null) {
				source.chat(String.format(this.noPermissionFormat, prefixed, message.getUser()));
			}

			return;
		}

		if(args.length < command.min() || (command.max() != -1 && args.length > command.max())) {
			if(this.incorrectUsageFormat != null) {
				source.chat(String.format(this.incorrectUsageFormat, prefixed, message.getUser()));
			}

			if(this.usageFormat != null) {
				source.chat(String.format(this.usageFormat, prefixed + " " + command.usage()));
			}

			return;
		}

		exec.execute(source, message.getUser(), cmd, args);
	}

	private ExecutionInfo getCommand(String name) {
		for(CommandExecutor exec : this.executors) {
			for(Method m : exec.getClass().getDeclaredMethods()) {
				Command cmd = m.getAnnotation(Command.class);
				if(cmd != null) {
					for(String alias : cmd.aliases()) {
						if(alias.equalsIgnoreCase(name)) {
							return new ExecutionInfo(m, cmd, exec);
						}
					}
				}
			}
		}

		return null;
	}

	private static class ExecutionInfo {
		private Method method;
		private Command cmd;
		private CommandExecutor executor;

		public ExecutionInfo(Method method, Command cmd, CommandExecutor executor) {
			this.method = method;
			this.cmd = cmd;
			this.executor = executor;
		}

		public void execute(Module source, String sender, String alias, String args[]) {
			try {
				this.method.invoke(this.executor, source, sender, alias, args);
			} catch(Exception e) {
				System.err.println("Error executing command.");
				e.printStackTrace();
			}
		}

		public Command getCommand() {
			return this.cmd;
		}
	}

}
