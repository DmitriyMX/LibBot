package org.spacehq.libbot.chat.cmd;

import org.spacehq.libbot.chat.ChatData;
import org.spacehq.libbot.module.Module;

import java.util.ArrayList;
import java.util.List;

public class ArgsExecutor implements Module {
	private String username;
	private StringBuilder result = new StringBuilder();

	public ArgsExecutor(String username) {
		this.username = username;
	}

	public String getResult() {
		return this.result.toString().trim().replace("\n", " ");
	}

	@Override
	public void connect() {
	}

	@Override
	public void disconnect(String reason) {
	}

	@Override
	public String getUsername() {
		return this.username;
	}

	@Override
	public void setUsername(String name) {
	}

	@Override
	public String getMessagePrefix() {
		return "[Pipe]";
	}

	@Override
	public List<ChatData> getIncomingChat() {
		return new ArrayList<ChatData>();
	}

	@Override
	public void chat(String message) {
		this.result.append(message).append("\n");
	}

	@Override
	public void update() {
	}
}
