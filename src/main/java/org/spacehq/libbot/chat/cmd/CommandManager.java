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

	public void register(CommandExecutor exec) {
		this.executors.add(exec);
	}

	public void remove(CommandExecutor exec) {
		this.executors.remove(exec);
	}

	public void execute(Module source, ChatData message) {
		String msg = message.getMessage().substring(this.prefix.length());
		String cmd = this.parser.getCommand(msg);
		String args[] = this.parser.getArguments(msg);
		ExecutionInfo exec = this.getCommand(cmd);
		if(exec == null) {
			source.chat("Unknown command, " + message.getUser() + ".");
			return;
		}

		Command command = exec.getCommand();
		if(!this.permManager.hasPermission(message.getUser(), command.permission())) {
			source.chat("You don't have permission to use \"" + cmd + "\", " + message.getUser() + ".");
			return;
		}

		if(args.length < command.min() || (command.max() != -1 && args.length > command.max())) {
			source.chat("Incorrect usage of " + this.prefix + cmd + ", " + message.getUser() + ".");
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
