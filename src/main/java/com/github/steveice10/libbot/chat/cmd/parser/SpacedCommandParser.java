package com.github.steveice10.libbot.chat.cmd.parser;

/**
 * Parses a command with spaces separating each part.
 *
 * command arg1 arg2 arg3...
 */
public class SpacedCommandParser implements CommandParser {
    @Override
    public String getCommand(String message) {
        return message.substring(0, message.contains(" ") ? message.indexOf(" ") : message.length());
    }

    @Override
    public String[] getArguments(String message) {
        return message.contains(" ") ? message.substring(message.indexOf(" ") + 1).split(" ") : new String[0];
    }
}
