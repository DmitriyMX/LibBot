package org.spacehq.libbot.chat.cmd.builtin;

import org.spacehq.libbot.chat.cmd.Command;
import org.spacehq.libbot.chat.cmd.CommandExecutor;
import org.spacehq.libbot.chat.cmd.CommandManager;
import org.spacehq.libbot.module.Module;

import java.util.Arrays;
import java.util.List;

public class HelpCommand implements CommandExecutor {
	private int linesPerPage;

	public HelpCommand(int linesPerPage) {
		this.linesPerPage = linesPerPage;
	}

	@Command(aliases = {"help", "cmds", "commands"}, desc = "Shows a list of all bot commands.", usage = "[page]", min = 0, max = 1, permission = "bot.help")
	public void help(Module source, CommandManager commands, String sender, String alias, String args[]) {
		List<Command> cmds = commands.getCommands(source, sender);
		int pages = (int) Math.ceil(cmds.size() / (float) this.linesPerPage);
		if(pages == 0) {
			source.chat("No commands are available for you to use, " + sender + ".");
		}

		int page = 0;
		if(args.length > 0) {
			try {
				page = Integer.parseInt(args[0]) - 1;
			} catch(NumberFormatException e) {
				source.chat("Invalid page \"" + args[0] + "\"");
				return;
			}
		}

		if(page < 0) {
			page = 0;
		}

		if(page >= pages) {
			page = pages - 1;
		}

		source.chat("Available commands of " + sender + " (Page " + (page + 1) + " out of " + pages + "):");
		for(int index = page * this.linesPerPage; index < (page + 1) * this.linesPerPage && index < cmds.size(); index++) {
			Command cmd = cmds.get(index);
			String array = Arrays.toString(cmd.aliases());
			String aliases = cmd.aliases().length > 1 ? "(" + array.substring(1, array.length() - 1) + ")" : cmd.aliases()[0];
			source.chat(" - " + commands.getPrefix() + aliases + " " + cmd.usage() + ": " + cmd.desc());
		}
	}
}
