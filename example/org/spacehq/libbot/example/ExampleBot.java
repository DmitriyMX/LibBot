package org.spacehq.libbot.example;

import org.spacehq.libbot.Bot;
import org.spacehq.libbot.chat.ChatData;
import org.spacehq.libbot.chat.cmd.builtin.HelpCommand;
import org.spacehq.libbot.chat.cmd.parser.SpacedCommandParser;
import org.spacehq.libbot.module.Module;
import org.spacehq.libbot.module.builtin.*;

public class ExampleBot extends Bot {
	private static final boolean MINECRAFT = true;
	private static final boolean MINECRAFT_CLASSIC = true;
	private static final boolean IRC = true;
	private static final boolean SKYPE = true;
	private static final boolean SLACK = true;
	private static final boolean HIP_CHAT = true;

	public static void main(String args[]) {
		new ExampleBot().start(args, true);
	}

	@Override
	public void initBot(String[] args) {
		this.getCommandManager().setPrefix("#");
		this.getCommandManager().setParser(new SpacedCommandParser());
		this.getCommandManager().register(new HelpCommand(10));
		this.getCommandManager().register(new ExampleCommands());
		this.addModule("Console", new ConsoleModule(this));
		if(MINECRAFT) {
			this.addModule("Minecraft", new MinecraftModule("localhost", 25565, "Username", "Password"));
		}

		if(MINECRAFT_CLASSIC) {
			this.addModule("MinecraftClassic", new MinecraftClassicModule("Username", "Password", "Server URL"));
		}

		if(IRC) {
			this.addModule("IRC", new IRCModule("ExampleBot", "localhost", "#channel"));
		}

		if(SKYPE) {
			this.addModule("Skype", new SkypeModule("Skype chat title", true));
		}

		if(SLACK) {
			this.addModule("Slack", new SlackModule("Token", "#channel", "Username"));
		}

		if(HIP_CHAT) {
			this.addModule("HipChat", new HipChatModule("Token", "Room", "Username"));
		}
	}

	@Override
	public void shutdownBot() {
	}

	@Override
	public void onChat(String moduleId, Module module, ChatData data) {
	}

	@Override
	public boolean onCommand(String moduleId, Module module, ChatData data) {
		return true;
	}
}
