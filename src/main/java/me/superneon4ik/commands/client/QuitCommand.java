package me.superneon4ik.commands.client;

import java.util.List;

import me.superneon4ik.commands.CommandExecutor;

public class QuitCommand implements CommandExecutor {

    @Override
    public boolean match(String label) {
        return label.equals("quit");
    }

    @Override
    public boolean execute(String label, List<String> args) {
        System.out.println("Use CTRL+C to quit.");
        return true;
    }
    
}
