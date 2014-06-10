package org.spacehq.libbot.chat.cmd.parser;

public interface CommandParser {
	public String getCommand(String message);

	public String[] getArguments(String message);
}
