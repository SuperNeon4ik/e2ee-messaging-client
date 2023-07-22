package me.superneon4ik.commands.client;

import java.util.Base64;
import java.util.List;

import me.superneon4ik.MessagingClient;
import me.superneon4ik.commands.CommandExecutor;

public class UsersCommand implements CommandExecutor {

    private final MessagingClient client;

    public UsersCommand(MessagingClient client) {
        this.client = client;
    }

    @Override
    public boolean match(String label) {
        return label.equals("users");
    }

    @Override
    public boolean execute(String label, List<String> args) {
        System.out.println(String.format("There are %d users in the chat:", client.getPublicKeys().size()));
        for (String username : client.getPublicKeys().keySet()) {
            System.out.println(String.format("- %s: %s", username, Base64.getEncoder().encodeToString(client.getPublicKeys().get(username).getEncoded())));
        }
        return true;
    }
    
}
