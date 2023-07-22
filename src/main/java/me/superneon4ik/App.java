package me.superneon4ik;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

@Command(name = "App", version = "App 1.0-SNAPSHOT", mixinStandardHelpOptions = true)
public class App implements Runnable {

    private static final Logger LOGGER = LogManager.getLogger(App.class);

    @Option(names = { "-a", "--address" }, description = "address to listen on") 
    String address = "127.0.0.1";
    @Option(names = { "-p", "--port" }, description = "port to listen on") 
    int port = 8080;
    @Option(names = { "-s", "--server" }, description = "turn on the server instead of client", defaultValue = "false")
    boolean isServer = false;

    @Override
    public void run() {
        if (isServer) {
            startServer();
        } else {
            startClient();
        }
    }

    public static void main(String[] args) {
        System.exit(new CommandLine(new App()).execute(args));
    }

    private void startServer() {
        LOGGER.info(String.format("Starting server on %s:%d!", address, port));
        try {
            MessagingServer server = new MessagingServer(address, port);
            server.run();
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.fatal("Failed to start the server.");
        }
    }

    private void startClient() {
        String username = System.console().readLine("Your username: ");
        LOGGER.info(String.format("Connecting to %s:%d!", address, port));
        try {
            MessagingClient client = new MessagingClient(username, address, port);
            client.run();
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.fatal("Failed to start the client.");
        }
    }
}
