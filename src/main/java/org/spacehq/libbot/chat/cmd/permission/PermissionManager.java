package org.spacehq.libbot.chat.cmd.permission;

import org.spacehq.libbot.module.Module;

public interface PermissionManager {
	public boolean hasPermission(Module source, String user, String permission);

	public boolean setPermission(Module source, String user, String permission, boolean has);
}
