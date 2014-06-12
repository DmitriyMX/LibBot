package org.spacehq.libbot.example;

import org.spacehq.libbot.chat.cmd.Command;
import org.spacehq.libbot.chat.cmd.CommandExecutor;
import org.spacehq.libbot.chat.cmd.CommandManager;
import org.spacehq.libbot.module.Module;

public class ExampleCommands implements CommandExecutor {
	@Command(aliases = { "hello" }, desc = "Says hello!", min = 2, usage = "<hi> <hi>", permission = "example.hello")
	public void hello(Module source, CommandManager commands, String sender, String alias, String args[]) {
		if(args[0].equals("hi") && args[1].equals("hi")) {
			source.chat("Hello there, " + sender + "!");
		} else {
			source.chat(sender + ", You need to use \"hi\" as the first and second arguments!");
			source.chat("You put: " + args[0] + ", " + args[1]);
		}
	}
}
