package org.spacehq.libbot.chat.cmd;

import org.spacehq.libbot.chat.ChatData;
import org.spacehq.libbot.module.Module;

import java.util.List;

/**
 * Used when executing a command within arguments to capture command output.
 */
public class ArgsExecutor implements Module {
    private Module source;
    private StringBuilder result = new StringBuilder();

    /**
     * Creates a new ArgsExecutor instance.
     *
     * @param source Source module of the argument's parent command.
     */
    public ArgsExecutor(Module source) {
        this.source = source;
    }

    /**
     * Gets the resulting command output of this executor.
     *
     * @return The resulting command output of this executor.
     */
    public String getResult() {
        return this.result.toString().trim().replace("\n", " ");
    }

    @Override
    public String getId() {
        return this.source.getId();
    }

    @Override
    public boolean isConnected() {
        return this.source.isConnected();
    }

    @Override
    public void connect() {
        this.source.connect();
    }

    @Override
    public void disconnect(String reason) {
        this.source.disconnect(reason);
    }

    @Override
    public String getUsername() {
        return this.source.getUsername();
    }

    @Override
    public void setUsername(String name) {
        this.source.setUsername(name);
    }

    @Override
    public List<ChatData> getIncomingChat() {
        return this.source.getIncomingChat();
    }

    @Override
    public void chat(String message) {
        this.result.append(message).append("\n");
    }
}
