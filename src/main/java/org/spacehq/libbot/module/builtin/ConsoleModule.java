package org.spacehq.libbot.module.builtin;

import org.spacehq.libbot.Bot;
import org.spacehq.libbot.chat.ChatData;
import org.spacehq.libbot.module.Module;
import org.spacehq.libbot.util.Conditions;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Module allowing commands to be executed from the console.
 */
public class ConsoleModule implements Module {
    private String id;
    private Bot bot;

    private List<ChatData> incoming = new ArrayList<ChatData>();
    private Thread thread;

    /**
     * Creates a new ConsoleModule instance.
     *
     * @param id  ID of the module.
     * @param bot Bot the module belongs to.
     */
    public ConsoleModule(String id, Bot bot) {
        Conditions.notNullOrEmpty(id, "Id");
        Conditions.notNull(bot, "Bot");

        this.id = id;
        this.bot = bot;
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public boolean isConnected() {
        return this.thread != null;
    }

    @Override
    public void connect() {
        this.thread = new Thread(new ConsoleReader(), "ConsoleReader");
        this.thread.start();
    }

    @Override
    public void disconnect(String reason) {
        this.thread.interrupt();
        this.thread = null;
    }

    @Override
    public String getUsername() {
        return "Console";
    }

    @Override
    public void setUsername(String name) {
        throw new UnsupportedOperationException("ConsoleModule does not have a username.");
    }

    @Override
    public List<ChatData> getIncomingChat() {
        List<ChatData> ret = new ArrayList<ChatData>(this.incoming);
        this.incoming.removeAll(ret);
        return ret;
    }

    @Override
    public void chat(String message) {
        System.out.println(message);
    }

    private class ConsoleReader implements Runnable {
        private boolean reading = true;

        public void stopReading() {
            this.reading = false;
        }

        @Override
        public void run() {
            BufferedReader read = new BufferedReader(new InputStreamReader(System.in));
            while(bot.isRunning()) {
                try {
                    if(read.ready()) {
                        String line = read.readLine();
                        if(line.startsWith(bot.getCommandManager().getPrefix())) {
                            incoming.add(new ChatData("Console", line));
                        } else {
                            incoming.add(new ChatData("Console", bot.getCommandManager().getPrefix() + line));
                        }
                    } else {
                        try {
                            Thread.sleep(100);
                        } catch(InterruptedException e) {
                            break;
                        }
                    }
                } catch(IOException e) {
                    System.err.println("Failed to read line from console!");
                    e.printStackTrace();
                }
            }
        }
    }
}
