package com.github.steveice10.libbot.module;

import com.github.steveice10.libbot.chat.ChatData;

import java.util.List;

/**
 * A module used to connect to a chat source.
 * <p>
 * Module methods may throw a ModuleException if a communication error occurs.
 */
public interface Module {
    /**
     * Gets the module's ID.
     *
     * @return The module's ID.
     */
    public String getId();

    /**
     * Gets whether the module is connected.
     *
     * @return Whether the module is connected.
     */
    public boolean isConnected();

    /**
     * Connects the module.
     */
    public void connect();

    /**
     * Disconnects the module.
     *
     * @param reason Reason for disconnecting.
     */
    public void disconnect(String reason);

    /**
     * Gets the username that the bot is acting through.
     *
     * @return The module's username.
     */
    public String getUsername();

    /**
     * Sets the username that the bot is acting through.
     *
     * @param name Username to set.
     * @throws UnsupportedOperationException if the module does not support changing its username.
     */
    public void setUsername(String name);

    /**
     * Gets a list of incoming chat messages.
     *
     * @return A list of incoming chat messages.
     */
    public List<ChatData> getIncomingChat();

    /**
     * Sends a chat message.
     *
     * @param message Message to send.
     */
    public void chat(String message);
}
