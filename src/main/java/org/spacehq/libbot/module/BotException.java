package org.spacehq.libbot.module;

/**
 * Exception thrown for bot-related errors.
 */
public class BotException extends RuntimeException {
    private static final long serialVersionUID = 1;

    public BotException() {
        super();
    }

    public BotException(String message) {
        super(message);
    }

    public BotException(Throwable cause) {
        super(cause);
    }

    public BotException(String message, Throwable cause) {
        super(message, cause);
    }
}
