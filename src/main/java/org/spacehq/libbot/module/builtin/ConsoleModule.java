package org.spacehq.libbot.module.builtin;

import org.spacehq.libbot.chat.ChatData;
import org.spacehq.libbot.module.Module;
import org.spacehq.libbot.util.Conditions;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Module allowing commands to be executed from the console.
 */
public class ConsoleModule implements Module {
    private String id;

    private List<ChatData> incoming = new ArrayList<ChatData>();
    private boolean running = false;

    /**
     * Creates a new ConsoleModule instance.
     *
     * @param id  ID of the module.
     */
    public ConsoleModule(String id) {
        Conditions.notNullOrEmpty(id, "Id");

        this.id = id;
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public boolean isConnected() {
        return this.running;
    }

    @Override
    public void connect() {
        new Thread(new ConsoleReader(), "ConsoleReader").start();
        this.running = true;
    }

    @Override
    public void disconnect(String reason) {
        this.running = false;
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
        if(this.incoming.isEmpty()) {
            return Collections.emptyList();
        }

        List<ChatData> ret = new ArrayList<ChatData>(this.incoming);
        this.incoming.removeAll(ret);
        return ret;
    }

    @Override
    public void chat(String message) {
        System.out.println(message);
    }

    private class ConsoleReader implements Runnable {
        @Override
        public void run() {
            BufferedReader read = new BufferedReader(new InputStreamReader(System.in));
            while(isConnected()) {
                try {
                    if(read.ready()) {
                        incoming.add(new ChatData("Console", read.readLine()));
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
