package com.github.steveice10.libbot.module.builtin;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.github.steveice10.libbot.chat.ChatData;
import com.github.steveice10.libbot.module.BotException;
import com.github.steveice10.libbot.module.Module;
import com.github.steveice10.libbot.util.Conditions;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

/**
 * Module for connecting to a HipChat room.
 */
public class HipChatModule implements Module {
    private static final String BASE_URL = "https://www.hipchat.com/v1/";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private String id;
    private String token;
    private String room;
    private String username;

    private String roomId;
    private long lastReceived;
    private List<ChatData> incoming = new ArrayList<ChatData>();

    private long lastUpdate;

    /**
     * Creates a new HipChatModule instance.
     *
     * @param id       ID of the module.
     * @param token    HipChat API token to use.
     * @param room     Room to connect to.
     * @param username Username to connect with.
     */
    public HipChatModule(String id, String token, String room, String username) {
        Conditions.notNullOrEmpty(id, "Id");
        Conditions.notNullOrEmpty(token, "Token");
        Conditions.notNullOrEmpty(room, "Room");
        Conditions.notNullOrEmpty(username, "Username");

        this.id = id;
        this.token = token;
        this.room = room;
        this.username = username;
    }

    @Override
    public String getId() {
        return this.id;
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
            throw new BotException("Could not find room \"" + this.room + "\".");
        }

        this.lastReceived = System.currentTimeMillis();

        new Thread(new HipChatUpdater()).start();
    }

    @Override
    public void disconnect(String reason) {
        if(this.roomId != null) {
            System.out.println("[" + this.id + "] Disconnected: " + reason);
            this.roomId = null;
            this.lastReceived = 0;
        }
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
    public List<ChatData> getIncomingChat() {
        if(this.incoming.isEmpty()) {
            return Collections.emptyList();
        }

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
            throw new BotException("Failed to call HipChat API method \"" + method + "\".", e);
        } finally {
            if(in != null) {
                try {
                    in.close();
                } catch(IOException e) {
                }
            }
        }
    }

    private class HipChatUpdater implements Runnable {
        @Override
        public void run() {
            while(isConnected()) {
                JsonObject history = call("rooms/history", "room_id", roomId, "date", "recent", "timezone", Calendar.getInstance().getTimeZone().getID());
                long latest = lastReceived;
                for(JsonElement e : history.get("messages").getAsJsonArray()) {
                    JsonObject message = e.getAsJsonObject();
                    String user = message.get("from").getAsJsonObject().get("name").getAsString();
                    String date = message.get("date").getAsString().replace("T", " ");
                    long timestamp;
                    try {
                        int end = date.contains("+") ? date.lastIndexOf("+") : date.contains("-") ? date.lastIndexOf("-") : date.length();
                        timestamp = DATE_FORMAT.parse(date.substring(0, end)).getTime();
                    } catch(ParseException e1) {
                        throw new BotException("Failed to parse date.", e1);
                    }

                    String text = message.get("message").getAsString();
                    if(timestamp > lastReceived) {
                        incoming.add(new ChatData(user, text));
                        if(timestamp > latest) {
                            latest = timestamp;
                        }
                    }
                }

                lastReceived = latest;

                // Make sure we don't get rate limited by limiting updates to 50 out of 100 requests per 5 minutes. The other 50 can be used for chatting, etc.
                try {
                    Thread.sleep(6000);
                } catch(InterruptedException e) {
                    break;
                }
            }
        }
    }
}
