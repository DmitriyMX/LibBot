package org.spacehq.libbot.chat.cmd.permission;

public class EmptyPermissionManager implements PermissionManager {

	@Override
	public boolean hasPermission(String user, String permission) {
		return false;
	}

}
