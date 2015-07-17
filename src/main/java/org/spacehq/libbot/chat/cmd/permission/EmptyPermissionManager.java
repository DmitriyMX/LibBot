package org.spacehq.libbot.chat.cmd.permission;

import org.spacehq.libbot.module.Module;

/**
 * An empty permission manager that grants all permissions to every user.
 */
public class EmptyPermissionManager implements PermissionManager {
    @Override
    public boolean hasPermission(Module source, String user, String permission) {
        return true;
    }

    @Override
    public boolean setPermission(Module source, String user, String permission, boolean has) {
        return true;
    }
}
