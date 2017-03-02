package com.github.steveice10.libbot.example;

import com.github.steveice10.libbot.Bot;
import com.github.steveice10.libbot.chat.ChatData;
import com.github.steveice10.libbot.chat.cmd.builtin.HelpCommand;
import com.github.steveice10.libbot.chat.cmd.parser.SpacedCommandParser;
import com.github.steveice10.libbot.module.Module;
import com.github.steveice10.libbot.module.builtin.ConsoleModule;
import com.github.steveice10.libbot.module.builtin.HipChatModule;
import com.github.steveice10.libbot.module.builtin.IRCModule;
import com.github.steveice10.libbot.module.builtin.MinecraftClassicModule;
import com.github.steveice10.libbot.module.builtin.MinecraftModule;
import com.github.steveice10.libbot.module.builtin.SkypeModule;
import com.github.steveice10.libbot.module.builtin.SlackModule;

public class ExampleBot extends Bot {
    private static final boolean CONSOLE = true;
    private static final boolean MINECRAFT = true;
    private static final boolean MINECRAFT_CLASSIC = true;
    private static final boolean IRC = true;
    private static final boolean SKYPE = true;
    private static final boolean SLACK = true;
    private static final boolean HIP_CHAT = true;

    public static void main(String args[]) {
        new ExampleBot().start(args);
    }

    @Override
    public void initBot(String[] args) {
        this.getCommandManager().setAcceptCommandsFromSelf(true);
        this.getCommandManager().setPrefix("#");
        this.getCommandManager().setParser(new SpacedCommandParser());
        this.getCommandManager().register(new HelpCommand(10));
        this.getCommandManager().register(new ExampleCommands());

        if(CONSOLE) {
            this.addModule(new ConsoleModule("Console"));
        }

        if(MINECRAFT) {
            this.addModule(new MinecraftModule("Minecraft", "Username", "Password", "localhost", 25565));
        }

        if(MINECRAFT_CLASSIC) {
            this.addModule(new MinecraftClassicModule("MinecraftClassic", "Username", "Password", "Server URL"));
        }

        if(IRC) {
            this.addModule(new IRCModule("IRC", "ExampleBot", "localhost", "#channel"));
        }

        if(SKYPE) {
            this.addModule(new SkypeModule("Skype", "Username", "Password", "Skype chat ID"));
        }

        if(SLACK) {
            this.addModule(new SlackModule("Slack", "Token", "#channel", "Username"));
        }

        if(HIP_CHAT) {
            this.addModule(new HipChatModule("HipChat", "Token", "Room", "Username"));
        }
    }

    @Override
    public void shutdownBot() {
    }

    @Override
    public void onChat(Module module, ChatData data) {
    }

    @Override
    public boolean onCommand(Module module, ChatData data) {
        return true;
    }
}
