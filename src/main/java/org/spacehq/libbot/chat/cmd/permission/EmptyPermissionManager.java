package org.spacehq.libbot.chat.cmd.permission;

import org.spacehq.libbot.module.Module;

public class EmptyPermissionManager implements PermissionManager {
	@Override
	public boolean hasPermission(Module source, String user, String permission) {
		return true;
	}
}
