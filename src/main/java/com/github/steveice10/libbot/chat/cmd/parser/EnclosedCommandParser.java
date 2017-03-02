package com.github.steveice10.libbot.chat.cmd.parser;

/**
 * Parses a command in the style of a method invocation.
 *
 * command(arg1, arg2, arg3...)
 */
public class EnclosedCommandParser implements CommandParser {
    @Override
    public String getCommand(String message) {
        return message.substring(0, message.contains("(") ? message.indexOf("(") : message.length());
    }

    @Override
    public String[] getArguments(String message) {
        return message.contains("(") && message.contains(")") ? message.substring(message.indexOf("(") + 1, message.indexOf(")")).split(", ") : new String[0];
    }
}
