package org.spacehq.libbot.chat;

public class ChatData {
	private String user;
	private String message;

	public ChatData(String user, String message) {
		this.user = user;
		this.message = message;
	}

	public String getUser() {
		return this.user;
	}

	public String getMessage() {
		return this.message;
	}
}
