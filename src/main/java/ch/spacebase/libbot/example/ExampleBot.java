package ch.spacebase.libbot.example;

import ch.spacebase.libbot.Bot;
import ch.spacebase.libbot.chat.cmd.parser.EnclosedCommandParser;
import ch.spacebase.libbot.module.builtin.ConsoleModule;
import ch.spacebase.libbot.module.builtin.IRCModule;

public class ExampleBot extends Bot {

	public static void main(String args[]) {
		new ExampleBot().start("ExampleBot", "1.0", args);
	}
	
	@Override
	public void initBot(String[] args) {
		this.getCommandManager().setParser(new EnclosedCommandParser());
		this.getCommandManager().register(new ExampleCommands());
		this.addModule(new ConsoleModule(this));
		this.addModule(new IRCModule(this, "ExampleBot", "localhost", "#channel"));
	}

}
