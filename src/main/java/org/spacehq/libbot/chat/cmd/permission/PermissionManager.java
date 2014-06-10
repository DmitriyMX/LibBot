package org.spacehq.libbot.chat.cmd.permission;

public interface PermissionManager {
	public boolean hasPermission(String user, String permission);
}
