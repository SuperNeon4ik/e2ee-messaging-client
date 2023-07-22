package me.superneon4ik;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.AdaptiveRecvByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.GlobalEventExecutor;
import lombok.Getter;
import me.superneon4ik.commands.CommandExecutor;
import me.superneon4ik.commands.client.HelpCommand;
import me.superneon4ik.commands.client.QuitCommand;
import me.superneon4ik.commands.client.UsersCommand;
import me.superneon4ik.enums.PacketID;
import me.superneon4ik.protocol.MessagingClientHandler;

@Getter 
public class MessagingClient {
    
    private static final Logger LOGGER = LogManager.getLogger(MessagingClient.class);

    private final String name;
    private final String host;
    private final int port;

    private PublicKey publicKey;
    private PrivateKey privateKey;
    private final Map<String, PublicKey> publicKeys = new HashMap<>();
    private final ChannelGroup channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    private final List<CommandExecutor> commands = new ArrayList<>();

    public MessagingClient(String name, String host, int port) throws NoSuchAlgorithmException {
        this.name = name;
        this.host = host;
        this.port = port;

        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.genKeyPair();
        publicKey = keyPair.getPublic();
        privateKey = keyPair.getPrivate();

        commands.add(new HelpCommand());
        commands.add(new UsersCommand(this));
        commands.add(new QuitCommand());
    }

    public void run() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            executor.execute(() -> {
                try {
                    handleSocket();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            executor.execute(this::handleInput);
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } finally {
            executor.shutdown();
        }
    }

    private void handleSocket() throws Exception {
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            Bootstrap b = new Bootstrap();
            b.group(workerGroup);
            b.channel(NioSocketChannel.class);
            b.option(ChannelOption.SO_KEEPALIVE, true);
            b.option(ChannelOption.RCVBUF_ALLOCATOR, new AdaptiveRecvByteBufAllocator(1024*2, 32*1024, 1024*1024*2));
            b.handler(new ChannelInitializer<SocketChannel>() {
 
                @Override
                public void initChannel(SocketChannel ch) 
                  throws Exception {
                    ch.pipeline().addLast(
                        new MessagingClientHandler(MessagingClient.this));
                }
            });

            ChannelFuture f = b.connect(host, port).sync();

            f.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
        }
    }

    private void handleInput() {
        try {
            while (true) {
                String userInput = System.console().readLine();
                if (userInput != null && !userInput.isEmpty()) {
                    if (userInput.startsWith("!")) {
                        handleCommand(userInput);
                    } else {
                        sendMessage(userInput);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleCommand(String input) {
        String[] tokens = input.split(" ");
        String command = tokens[0].substring(1);
        List<String> args = Arrays.stream(tokens).skip(1).toList();

        var optExecutor = commands.stream().filter(c -> c.match(command)).findFirst();
        if (optExecutor.isEmpty()) {
            LOGGER.warn(String.format("No such command '%s'.", command));
            return;
        }

        boolean result = optExecutor.get().execute(command, args);
        if (!result) {
            LOGGER.error(String.format("Command '%s' failed to execute properly.", command));
        }
    }

    public void sendMessage(String message) throws Exception {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(256);
        SecretKey key = keyGenerator.generateKey();

        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] cipherText = cipher.doFinal(message.getBytes());

        for (Entry<String, PublicKey> entry : publicKeys.entrySet()) {
            Cipher rsaCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            rsaCipher.init(Cipher.ENCRYPT_MODE, entry.getValue());
            byte[] ciphertextKey = rsaCipher.doFinal(key.getEncoded());

            ByteBuf msgBuf = Unpooled.buffer();
            msgBuf.writeShort(PacketID.MESSAGE.getId());
            msgBuf.writeInt(entry.getKey().length());
            msgBuf.writeCharSequence(entry.getKey(), StandardCharsets.UTF_8);
            msgBuf.writeInt(ciphertextKey.length);
            msgBuf.writeBytes(ciphertextKey);
            msgBuf.writeInt(cipherText.length);
            msgBuf.writeBytes(cipherText);
            broadcast(msgBuf);
        }
    }

    public void broadcast(ByteBuf msg) {
        channels.writeAndFlush(msg);
    }
}
