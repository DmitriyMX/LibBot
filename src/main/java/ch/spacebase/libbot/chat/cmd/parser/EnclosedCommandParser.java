package ch.spacebase.libbot.chat.cmd.parser;

public class EnclosedCommandParser implements CommandParser {

	@Override
	public String getCommand(String message) {
		return message.substring(0, message.indexOf("(") != -1 ? message.indexOf("(") : message.length());
	}

	@Override
	public String[] getArguments(String message) {
		return message.indexOf("(") != -1 && message.indexOf(")") != -1 ? message.substring(message.indexOf("(") + 1, message.indexOf(")")).split(", ") : new String[0];
	}

}
