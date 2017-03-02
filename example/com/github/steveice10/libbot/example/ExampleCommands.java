package com.github.steveice10.libbot.example;

import com.github.steveice10.libbot.chat.cmd.Command;
import com.github.steveice10.libbot.chat.cmd.CommandManager;
import com.github.steveice10.libbot.module.Module;

public class ExampleCommands {
    @Command(aliases = { "hello" }, desc = "Says hello!", min = 2, usage = "<hi> <hi>", permission = "example.hello")
    public void hello(Module source, CommandManager commands, String sender, String alias, String args[]) {
        if(args[0].equals("hi") && args[1].equals("hi")) {
            source.chat("Hello there, " + sender + "!");
        } else {
            source.chat(sender + ", You need to use \"hi\" as the first and second arguments!");
            source.chat("You put: " + args[0] + ", " + args[1]);
        }
    }
}
