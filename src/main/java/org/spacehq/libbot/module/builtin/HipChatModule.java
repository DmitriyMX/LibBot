package org.spacehq.libbot.module.builtin;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.spacehq.libbot.chat.ChatData;
import org.spacehq.libbot.module.Module;
import org.spacehq.libbot.module.ModuleException;
import org.spacehq.libbot.util.Conditions;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class HipChatModule implements Module {
	private static final String BASE_URL = "https://www.hipchat.com/v1/";
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	private String token;
	private String room;
	private String username;

	private String roomId;
	private long lastReceived;
	private List<ChatData> incoming = new CopyOnWriteArrayList<ChatData>();

	public HipChatModule(String token, String room, String username) {
		Conditions.notNullOrEmpty(token, "Token");
		Conditions.notNullOrEmpty(room, "Room");
		Conditions.notNullOrEmpty(username, "Username");

		this.token = token;
		this.room = room;
		this.username = username;
	}

	@Override
	public boolean isConnected() {
		return this.roomId != null;
	}

	@Override
	public void connect() {
		JsonObject roomsList = this.call("rooms/list");
		for(JsonElement e : roomsList.get("rooms").getAsJsonArray()) {
			JsonObject room = e.getAsJsonObject();
			if(room.get("name").getAsString().equals(this.room)) {
				this.roomId = room.get("room_id").getAsString();
				break;
			}
		}

		if(this.roomId == null) {
			throw new ModuleException("Could not find room \"" + this.room + "\".");
		}

		this.lastReceived = System.currentTimeMillis();
	}

	@Override
	public void disconnect(String reason) {
		if(this.roomId == null) {
			return;
		}

		System.out.println(this.getMessagePrefix() + " Disconnected: " + reason);
		this.roomId = null;
		this.lastReceived = 0;
	}

	@Override
	public String getUsername() {
		return this.username;
	}

	@Override
	public void setUsername(String name) {
		Conditions.notNullOrEmpty(name, "Username");

		this.username = name;
	}

	@Override
	public String getMessagePrefix() {
		return "[HipChat - " + this.getUsername() + " - " + this.room + "]";
	}

	@Override
	public List<ChatData> getIncomingChat() {
		List<ChatData> incoming = new ArrayList<ChatData>(this.incoming);
		this.incoming.removeAll(incoming);
		return incoming;
	}

	@Override
	public void chat(String message) {
		if(this.roomId == null) {
			return;
		}

		for(String msg : message.split("\n")) {
			this.call("rooms/message", "room_id", this.roomId, "from", this.username, "message", msg, "message_format", "text");
		}
	}

	@Override
	public void update() {
		if(this.roomId == null) {
			return;
		}

		JsonObject history = this.call("rooms/history", "room_id", this.roomId, "date", "recent", "timezone", Calendar.getInstance().getTimeZone().getID());
		long latest = this.lastReceived;
		for(JsonElement e : history.get("messages").getAsJsonArray()) {
			JsonObject message = e.getAsJsonObject();
			String user = message.get("from").getAsJsonObject().get("name").getAsString();
			String date = message.get("date").getAsString().replace("T", " ");
			long timestamp;
			try {
				int end = date.contains("+") ? date.lastIndexOf("+") : date.contains("-") ? date.lastIndexOf("-") : date.length();
				timestamp = DATE_FORMAT.parse(date.substring(0, end)).getTime();
			} catch(ParseException e1) {
				throw new ModuleException("Failed to parse date.", e1);
			}

			String text = message.get("message").getAsString();
			if(timestamp > this.lastReceived) {
				this.incoming.add(new ChatData(user, text));
				if(timestamp > latest) {
					latest = timestamp;
				}
			}
		}

		this.lastReceived = latest;
		// Make sure we don't get rate limited by limiting updates to 50 out of 100 requests per 5 minutes. The other 50 can be used for chatting, etc.
		try {
			Thread.sleep(6000);
		} catch(InterruptedException e) {
		}
	}

	private JsonObject call(String method, String... params) {
		InputStream in = null;
		try {
			StringBuilder build = new StringBuilder();
			build.append("?format=json&auth_token=").append(URLEncoder.encode(this.token, "UTF-8"));
			for(int index = 0; index < params.length; index += 2) {
				build.append("&").append(params[index]).append("=").append(URLEncoder.encode(params[index + 1], "UTF-8"));
			}

			in = new URL(BASE_URL + method + build.toString()).openStream();
			return new Gson().fromJson(new InputStreamReader(in), JsonObject.class);
		} catch(Exception e) {
			throw new ModuleException("Failed to call HipChat API method \"" + method + "\".", e);
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
