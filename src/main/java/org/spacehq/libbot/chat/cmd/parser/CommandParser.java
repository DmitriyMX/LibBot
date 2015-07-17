package org.spacehq.libbot.chat.cmd.parser;

/**
 * Parses command details from a chat message.
 */
public interface CommandParser {
    /**
     * Gets the command used from a chat message.
     *
     * @param message Message to parse.
     * @return The command used in the chat message.
     */
    public String getCommand(String message);

    /**
     * Gets the command arguments provided in a chat message.
     *
     * @param message Message to parse.
     * @return The command arguments contained in the chat message.
     */
    public String[] getArguments(String message);
}
