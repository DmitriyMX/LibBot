package org.spacehq.libbot.module.builtin;

import com.google.gson.*;
import org.spacehq.libbot.chat.ChatData;
import org.spacehq.libbot.module.Module;
import org.spacehq.libbot.module.ModuleException;
import org.spacehq.libbot.util.HtmlEscaping;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class SlackModule implements Module {
	private static final String BASE_URL = "https://slack.com/api/";

	private String token;
	private String channel;
	private String username;

	private String channelId;
	private double lastReceived;
	private Map<String, String> users = new HashMap<String, String>();
	private List<ChatData> incoming = new CopyOnWriteArrayList<ChatData>();

	public SlackModule(String token, String channel, String username) {
		this.token = token;
		this.channel = channel;
		this.username = username;
	}

	@Override
	public void connect() {
		this.channelId = this.call("channels.join", "name", this.channel).get("channel").getAsJsonObject().get("id").getAsString();
		this.lastReceived = System.currentTimeMillis() / 1000.0;
	}

	@Override
	public void disconnect(String reason) {
		if(this.channelId == null) {
			return;
		}

		System.out.println(this.getMessagePrefix() + " Disconnected: " + reason);
		this.call("channels.leave", "channel", this.channelId);
		this.channelId = null;
		this.lastReceived = 0;
		this.users.clear();
	}

	@Override
	public String getUsername() {
		return this.username;
	}

	@Override
	public void setUsername(String name) {
		this.username = name;
	}

	@Override
	public String getMessagePrefix() {
		return "[Slack]";
	}

	@Override
	public List<ChatData> getIncomingChat() {
		List<ChatData> incoming = new ArrayList<ChatData>(this.incoming);
		this.incoming.removeAll(incoming);
		return incoming;
	}

	@Override
	public void chat(String message) {
		if(this.channelId == null) {
			return;
		}

		for(String msg : message.split("\n")) {
			this.call("chat.postMessage", "channel", this.channelId, "text", msg, "username", this.username);
		}
	}

	@Override
	public void update() {
		if(this.channelId == null) {
			return;
		}

		this.users.clear();
		JsonObject users = this.call("users.list");
		for(JsonElement e : users.get("members").getAsJsonArray()) {
			JsonObject member = e.getAsJsonObject();
			this.users.put(member.get("id").getAsString(), member.get("name").getAsString());
		}

		JsonObject history = this.call("channels.history", "channel", this.channelId, "count", String.valueOf(10));
		double latest = this.lastReceived;
		JsonArray messages = history.get("messages").getAsJsonArray();
		for(int index = messages.size() - 1; index >= 0; index--) {
			JsonObject message = messages.get(index).getAsJsonObject();
			String user = message.has("user") ? message.get("user").getAsString() : message.get("username").getAsString();
			double timestamp = message.get("ts").getAsDouble();
			String text = HtmlEscaping.unescape(message.get("text").getAsString()).replaceAll("<@U(\\w+)\\|(\\w+)>", "$2");
			if(message.get("type").getAsString().equals("message") && timestamp > this.lastReceived) {
				this.incoming.add(new ChatData(this.users.containsKey(user) ? this.users.get(user) : user, text));
				if(timestamp > latest) {
					latest = timestamp;
				}
			}
		}

		this.lastReceived = latest;
	}

	private JsonObject call(String method, String... params) {
		InputStream in = null;
		try {
			StringBuilder build = new StringBuilder();
			build.append("?token=").append(URLEncoder.encode(this.token, "UTF-8"));
			for(int index = 0; index < params.length; index += 2) {
				build.append("&").append(params[index]).append("=").append(URLEncoder.encode(params[index + 1], "UTF-8"));
			}

			in = new URL(BASE_URL + method + build.toString()).openStream();
			JsonObject json = new Gson().fromJson(new InputStreamReader(in), JsonObject.class);
			JsonPrimitive ok = json.get("ok").getAsJsonPrimitive();
			if((ok.isBoolean() && !json.get("ok").getAsBoolean()) || (ok.isNumber() && json.get("ok").getAsInt() != 1)) {
				throw new ModuleException("Failed to call Slack API method \"" + method + "\": " + json.get("error").getAsString());
			}

			return json;
		} catch(Exception e) {
			throw new ModuleException("Failed to call Slack API method \"" + method + "\".", e);
		} finally {
			if(in != null) {
				try {
					in.close();
				} catch(IOException e) {
				}
			}
		}
	}
}
