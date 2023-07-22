package me.superneon4ik.protocol;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import me.superneon4ik.MessagingClient;
import me.superneon4ik.enums.PacketID;

public class MessagingClientHandler extends ChannelInboundHandlerAdapter {

    private static final Logger LOGGER = LogManager.getLogger(MessagingClientHandler.class);
    private final MessagingClient client;

    public MessagingClientHandler(MessagingClient client) {
        this.client = client;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        // Share public key
        ByteBuf msg = ctx.alloc().buffer();
        msg.writeShort(PacketID.PUBKEY.getId());
        msg.writeInt(client.getName().length());
        msg.writeCharSequence(client.getName(), StandardCharsets.UTF_8);
        msg.writeInt(client.getPublicKey().getEncoded().length);
        msg.writeBytes(client.getPublicKey().getEncoded());
        ctx.writeAndFlush(msg);

        client.getChannels().add(ctx.channel());
        LOGGER.info("New connection registered.");
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf in = (ByteBuf) msg;
        int packetId = in.readShort();
        if (packetId == PacketID.PUBKEY.getId()) {
            int usernameLength = in.readInt();
            String username = in.readCharSequence(usernameLength, StandardCharsets.UTF_8).toString();
            int publicKeySize = in.readInt();
            byte[] publicKeyBytes = new byte[publicKeySize];
            in.readBytes(publicKeyBytes);

            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyBytes);
            PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);

            if (!client.getPublicKeys().containsKey(username)) {
                System.out.println(String.format("%s joined the chat.", username));

                if (username.equals(client.getName())) {
                    ByteBuf catchupReqBuf = ctx.alloc().buffer();
                    catchupReqBuf.writeShort(PacketID.CATCHUP.getId());
                    ctx.writeAndFlush(catchupReqBuf);
                    LOGGER.info("Requested pubkey catchup.");
                }
            }
            client.getPublicKeys().put(username, publicKey);
        } else if (packetId == PacketID.MESSAGE.getId()) {
            int usernameLength = in.readInt();
            String username = in.readCharSequence(usernameLength, StandardCharsets.UTF_8).toString();
            int encryptedKeyLength = in.readInt();
            byte[] encryptedKeyBytes = new byte[encryptedKeyLength];
            in.readBytes(encryptedKeyBytes);
            int encryptedMessageLength = in.readInt();
            byte[] encryptedMessageBytes = new byte[encryptedMessageLength];
            in.readBytes(encryptedMessageBytes);

            Cipher keyCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            keyCipher.init(Cipher.DECRYPT_MODE, client.getPrivateKey());
            byte[] keyBytes = keyCipher.doFinal(encryptedKeyBytes);
            SecretKey key = new SecretKeySpec(keyBytes, "AES");

            Cipher messageCipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            messageCipher.init(Cipher.DECRYPT_MODE, key);
            byte[] messageBytes = messageCipher.doFinal(encryptedMessageBytes);
            String message = new String(messageBytes, StandardCharsets.UTF_8);

            System.out.println(String.format("%s: %s", username, message));
        } else if (packetId == PacketID.GOODBYE.getId()) {
            int usernameLength = in.readInt();
            String username = in.readCharSequence(usernameLength, StandardCharsets.UTF_8).toString();

            client.getPublicKeys().remove(username);
            System.out.println(String.format("%s left the chat.", username));
        } else if (packetId == PacketID.CATCHUP.getId()) {
            int length = in.readInt();
            for (int i = 0; i < length; i++) {
                int usernameLength = in.readInt();
                String username = in.readCharSequence(usernameLength, StandardCharsets.UTF_8).toString();
                int publicKeySize = in.readInt();
                byte[] publicKeyBytes = new byte[publicKeySize];
                in.readBytes(publicKeyBytes);

                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyBytes);
                PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);

                client.getPublicKeys().put(username, publicKey);
            }

            System.out.println(String.format("There are %d users in the chat: %s", length, String.join(", ", client.getPublicKeys().keySet())));
        }

        // If there are any remaining bytes in the buffer, keep them for the next read
        if (in.readableBytes() > 0) {
            in.discardReadBytes();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        client.getChannels().remove(ctx.channel());
        LOGGER.info("Connection closed.");
        ctx.close();
    }
}
