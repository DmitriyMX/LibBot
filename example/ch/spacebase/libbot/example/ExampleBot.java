package ch.spacebase.libbot.example;

import ch.spacebase.libbot.Bot;
import ch.spacebase.libbot.chat.cmd.parser.EnclosedCommandParser;
import ch.spacebase.libbot.module.builtin.ConsoleModule;
import ch.spacebase.libbot.module.builtin.IRCModule;
import ch.spacebase.libbot.module.builtin.MinecraftModule;
import ch.spacebase.mc.auth.exception.AuthenticationException;

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
