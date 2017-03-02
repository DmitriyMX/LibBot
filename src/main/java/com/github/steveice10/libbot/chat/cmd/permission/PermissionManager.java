package com.github.steveice10.libbot.chat.cmd.permission;

import com.github.steveice10.libbot.module.Module;

/**
 * Manages the permissions of users.
 */
public interface PermissionManager {
    /**
     * Checks whether a user has the given permission.
     *
     * @param source     Module that the user communicates from.
     * @param user       User to check the permission for.
     * @param permission Permission to check.
     * @return Whether the user has the given permission.
     */
    public boolean hasPermission(Module source, String user, String permission);

    /**
     * Sets whether a user has the given permission.
     *
     * @param source     Module that the user communicates from.
     * @param user       User to set the permission for.
     * @param permission Permission to set.
     * @param has        Whether the user should have the given permission.
     * @return Whether the operation was successful.
     */
    public boolean setPermission(Module source, String user, String permission, boolean has);
}
