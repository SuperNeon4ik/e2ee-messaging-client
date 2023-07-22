package me.superneon4ik.commands;

import java.util.List;

public interface CommandExecutor {
    boolean match(String label);
    boolean execute(String label, List<String> args);
}
