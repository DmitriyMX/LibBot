package com.github.steveice10.libbot.chat.cmd;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Provides information about a command method.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Command {
    /**
     * Gets the command's aliases.
     *
     * @return The command's aliases.
     */
    public String[] aliases();

    /**
     * Gets the command's description.
     *
     * @return The command's description.
     */
    public String desc();

    /**
     * Gets the command's usage information.
     *
     * @return The command's usage information.
     */
    public String usage() default "";

    /**
     * Gets the permission required to use the command.
     *
     * @return The permission required to use the command.
     */
    public String permission();

    /**
     * Gets the minimum number of arguments required for the command.
     *
     * @return The minimum number of arguments required for the command.
     */
    public int min() default 0;

    /**
     * Gets the maximum number of arguments required for the command.
     *
     * @return The maximum number of arguments required for the command.
     */
    public int max() default -1;
}
