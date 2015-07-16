package org.spacehq.libbot.util;

import org.spacehq.libbot.module.BotException;

/**
 * Provides utility methods for checking if certain conditions are true.
 */
public class Conditions {
    /**
     * Throws an exception if the given object is null.
     * @param o Object to check.
     * @param name Name to use in the exception.
     * @throws BotException if the given object is null.
     */
    public static void notNull(Object o, String name) {
        if(o == null) {
            throw new BotException(name + " cannot be null.");
        }
    }

    /**
     * Throws an exception if the given string is null or empty.
     * @param s String to check.
     * @param name Name to use in the exception.
     * @throws BotException if the given string is null or empty.
     */
    public static void notNullOrEmpty(String s, String name) {
        if(s == null || s.isEmpty()) {
            throw new BotException(name + " cannot be null or empty.");
        }
    }
}
