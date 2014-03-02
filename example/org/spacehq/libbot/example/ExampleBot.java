package org.spacehq.libbot.example;

import org.spacehq.libbot.Bot;
import org.spacehq.libbot.chat.cmd.parser.EnclosedCommandParser;
import org.spacehq.libbot.module.builtin.ConsoleModule;
import org.spacehq.libbot.module.builtin.IRCModule;
import org.spacehq.libbot.module.builtin.MinecraftModule;
import org.spacehq.mc.auth.exception.AuthenticationException;

public class ExampleBot extends Bot {

	public static void main(String args[]) {
		new ExampleBot().start("ExampleBot", "1.0", args);
	}

	@Override
	public void initBot(String[] args) {
		this.getCommandManager().setParser(new EnclosedCommandParser());
		this.getCommandManager().register(new ExampleCommands());
		this.addModule(new ConsoleModule(this));
		try {
			this.addModule(new MinecraftModule(this, "localhost", 25565, "user", "pass"));
		} catch(AuthenticationException e) {
			System.err.println("Failed to authenticate Minecraft bot module!");
			e.printStackTrace();
		}

		this.addModule(new IRCModule(this, "ExampleBot", "localhost", "#channel"));
	}

}
