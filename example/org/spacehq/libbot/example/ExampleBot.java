package org.spacehq.libbot.example;

import org.spacehq.libbot.Bot;
import org.spacehq.libbot.chat.cmd.parser.SpacedCommandParser;
import org.spacehq.libbot.module.builtin.ConsoleModule;
import org.spacehq.libbot.module.builtin.IRCModule;
import org.spacehq.libbot.module.builtin.MinecraftModule;
import org.spacehq.libbot.module.builtin.SkypeModule;

public class ExampleBot extends Bot {
	private static final boolean MINECRAFT = true;
	private static final boolean IRC = true;
	private static final boolean SKYPE = true;

	public static void main(String args[]) {
		new ExampleBot().start("ExampleBot", "1.0", true, args);
	}

	@Override
	public void initBot(String[] args) {
		this.getCommandManager().setPrefix("#");
		this.getCommandManager().setParser(new SpacedCommandParser());
		this.getCommandManager().register(new ExampleCommands());
		this.addModule(new ConsoleModule(this));
		if(MINECRAFT) {
			this.addModule(new MinecraftModule(this, "localhost", 25565, "user", "pass"));
		}

		if(IRC) {
			this.addModule(new IRCModule(this, "ExampleBot", "localhost", "#channel"));
		}

		if(SKYPE) {
			this.addModule(new SkypeModule("Skype chat title"));
		}
	}
}
