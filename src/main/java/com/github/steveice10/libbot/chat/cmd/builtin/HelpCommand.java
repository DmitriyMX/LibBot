package com.github.steveice10.libbot.chat.cmd.builtin;

import com.github.steveice10.libbot.chat.cmd.Command;
import com.github.steveice10.libbot.chat.cmd.CommandManager;
import com.github.steveice10.libbot.module.Module;

import java.util.Arrays;
import java.util.List;

/**
 * Provides a command detailing the various commands a user has access to.
 */
public class HelpCommand {
    private int linesPerPage;

    /**
     * Creates a new HelpCommand instance.
     *
     * @param linesPerPage Number of help lines to display per page.
     */
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

        StringBuilder build = new StringBuilder();
        build.append("Available commands of ").append(sender).append(" (Page ").append(page + 1).append(" out of ").append(pages).append("):\n");
        for(int index = page * this.linesPerPage; index < (page + 1) * this.linesPerPage && index < cmds.size(); index++) {
            Command cmd = cmds.get(index);
            String array = Arrays.toString(cmd.aliases());
            String aliases = cmd.aliases().length > 1 ? "(" + array.substring(1, array.length() - 1) + ")" : cmd.aliases()[0];
            build.append(" - ").append(commands.getPrefix()).append(aliases).append(" ").append(cmd.usage()).append(": ").append(cmd.desc()).append("\n");
        }

        source.chat(build.toString().trim());
    }
}
