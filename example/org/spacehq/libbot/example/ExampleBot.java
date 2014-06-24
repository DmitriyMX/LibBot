package org.spacehq.libbot.example;

import org.spacehq.libbot.Bot;
import org.spacehq.libbot.chat.ChatData;
import org.spacehq.libbot.chat.cmd.builtin.HelpCommand;
import org.spacehq.libbot.chat.cmd.parser.SpacedCommandParser;
import org.spacehq.libbot.module.builtin.*;

public class ExampleBot extends Bot {
	private static final boolean MINECRAFT = true;
	private static final boolean MINECRAFT_CLASSIC = true;
	private static final boolean IRC = true;
	private static final boolean SKYPE = true;
	private static final boolean SLACK = true;
	private static final boolean HIP_CHAT = true;

	public static void main(String args[]) {
		new ExampleBot().start("ExampleBot", "1.0", true, args);
	}

	@Override
	public void initBot(String[] args) {
		this.getCommandManager().setPrefix("#");
		this.getCommandManager().setParser(new SpacedCommandParser());
		this.getCommandManager().register(new HelpCommand(10));
		this.getCommandManager().register(new ExampleCommands());
		this.addModule(new ConsoleModule(this));
		if(MINECRAFT) {
			this.addModule(new MinecraftModule("localhost", 25565, "Username", "Password"));
		}

		if(MINECRAFT_CLASSIC) {
			this.addModule(new MinecraftClassicModule("Username", "Password", "Server URL"));
		}

		if(IRC) {
			this.addModule(new IRCModule("ExampleBot", "localhost", "#channel"));
		}

		if(SKYPE) {
			this.addModule(new SkypeModule("Skype chat title"));
		}

		if(SLACK) {
			this.addModule(new SlackModule("Token", "#channel", "Username"));
		}

		if(HIP_CHAT) {
			this.addModule(new HipChatModule("Token", "Room", "Username"));
		}
	}

	@Override
	public void onChat(ChatData data) {
	}
}
