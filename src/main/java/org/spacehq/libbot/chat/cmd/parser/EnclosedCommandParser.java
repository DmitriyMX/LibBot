package org.spacehq.libbot.chat.cmd.parser;

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
