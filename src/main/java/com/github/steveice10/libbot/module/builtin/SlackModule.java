package com.github.steveice10.libbot.module.builtin;

import com.github.steveice10.libbot.chat.ChatData;
import com.github.steveice10.libbot.module.BotException;
import com.github.steveice10.libbot.module.Module;
import com.github.steveice10.libbot.util.Conditions;
import com.github.steveice10.libbot.util.HtmlEscaping;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Module for connecting to a Slack chat.
 */
public class SlackModule implements Module {
    private static final String BASE_URL = "https://slack.com/api/";

    private String id;
    private String token;
    private String channel;
    private String username;

    private String channelId;
    private double lastReceived;
    private Map<String, String> users = new HashMap<String, String>();
    private List<ChatData> incoming = new ArrayList<ChatData>();

    /**
     * Creates a new SlackModule instance.
     *
     * @param id       ID of the module.
     * @param token    Slack API token to use.
     * @param channel  Channel to connect to.
     * @param username Username to connect with.
     */
    public SlackModule(String id, String token, String channel, String username) {
        Conditions.notNullOrEmpty(id, "Id");
        Conditions.notNullOrEmpty(token, "Token");
        Conditions.notNullOrEmpty(channel, "Channel");
        Conditions.notNullOrEmpty(username, "Username");

        this.id = id;
        this.token = token;
        this.channel = channel;
        this.username = username;
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public boolean isConnected() {
        return this.channelId != null;
    }

    @Override
    public void connect() {
        this.channelId = this.call("channels.join", "name", this.channel).get("channel").getAsJsonObject().get("id").getAsString();
        this.lastReceived = System.currentTimeMillis() / 1000.0;

        new Thread(new SlackUpdater()).start();
    }

    @Override
    public void disconnect(String reason) {
        if(this.channelId != null) {
            System.out.println("[" + this.id + "] Disconnected: " + reason);
            this.call("channels.leave", "channel", this.channelId);
            this.channelId = null;
            this.lastReceived = 0;
            this.users.clear();
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
        if(this.channelId == null) {
            return;
        }

        for(String msg : message.split("\n")) {
            this.call("chat.postMessage", "channel", this.channelId, "text", msg, "username", this.username);
        }
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
                throw new BotException("Failed to call Slack API method \"" + method + "\": " + json.get("error").getAsString());
            }

            return json;
        } catch(Exception e) {
            throw new BotException("Failed to call Slack API method \"" + method + "\".", e);
        } finally {
            if(in != null) {
                try {
                    in.close();
                } catch(IOException e) {
                }
            }
        }
    }

    private class SlackUpdater implements Runnable {
        @Override
        public void run() {
            while(isConnected()) {
                users.clear();
                for(JsonElement e : call("users.list").get("members").getAsJsonArray()) {
                    JsonObject member = e.getAsJsonObject();
                    users.put(member.get("id").getAsString(), member.get("name").getAsString());
                }

                JsonObject history = call("channels.history", "channel", channelId, "count", String.valueOf(10));
                double latest = lastReceived;
                JsonArray messages = history.get("messages").getAsJsonArray();
                for(int index = messages.size() - 1; index >= 0; index--) {
                    JsonObject message = messages.get(index).getAsJsonObject();
                    if(message.has("message")) {
                        message = message.get("message").getAsJsonObject();
                    }

                    if(message.get("type").getAsString().equals("message")) {
                        String user = message.has("user") ? message.get("user").getAsString() : message.get("username").getAsString();
                        double timestamp = message.get("ts").getAsDouble();
                        String text = HtmlEscaping.unescape(message.get("text").getAsString()).replaceAll("<@U(\\w+)\\|(\\w+)>", "$2");
                        if(timestamp > lastReceived) {
                            incoming.add(new ChatData(users.containsKey(user) ? users.get(user) : user, text));
                            if(timestamp > latest) {
                                latest = timestamp;
                            }
                        }
                    }
                }

                lastReceived = latest;

                // Make sure we don't make requests too quickly.
                try {
                    Thread.sleep(1000);
                } catch(InterruptedException e) {
                    break;
                }
            }
        }
    }
}
