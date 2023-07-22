package me.superneon4ik.commands.client;

import java.util.List;

import me.superneon4ik.commands.CommandExecutor;

public class HelpCommand implements CommandExecutor {

    @Override
    public boolean match(String label) {
        return label.equals("help");
    }

    @Override
    public boolean execute(String label, List<String> args) {
        System.out.println("Current list of commands:");
        System.out.println("- !help");
        System.out.println("- !users");
        System.out.println("- !quit");
        return true;
    }
    
}
