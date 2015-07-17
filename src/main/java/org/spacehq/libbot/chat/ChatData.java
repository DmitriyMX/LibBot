package org.spacehq.libbot.chat;

/**
 * Contains the details of a chat message.
 */
public class ChatData {
    private String user;
    private String message;

    /**
     * Creates a new ChatData instance.
     *
     * @param user    User that sent the message.
     * @param message Message that was sent.
     */
    public ChatData(String user, String message) {
        this.user = user;
        this.message = message;
    }

    /**
     * Gets the user that sent the message.
     *
     * @return The user that sent the message.
     */
    public String getUser() {
        return this.user;
    }

    /**
     * Gets the message that was sent.
     *
     * @return The message that was sent.
     */
    public String getMessage() {
        return this.message;
    }
}
