package me.superneon4ik.protocol;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import me.superneon4ik.MessagingServer;
import me.superneon4ik.enums.PacketID;

public class MessagingServerHandler extends ChannelInboundHandlerAdapter {

    private static final Logger LOGGER = LogManager.getLogger(MessagingServerHandler.class);
    private final MessagingServer server;

    public MessagingServerHandler(MessagingServer client) {
        this.server = client;
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
            in.retain();

            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyBytes);
            PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);

            server.getPublicKeys().put(username, publicKey);
            server.getChannels().put(username, ctx.channel());

            LOGGER.info(String.format("%s's public key: %s", username, Base64.getEncoder().encodeToString(publicKey.getEncoded())));
            LOGGER.info(String.format("%s connected to the server", username));

            // Forward the packet to everyone
            ByteBuf forwardedMessage = Unpooled.buffer();
            forwardedMessage.writeShort(PacketID.PUBKEY.getId());
            forwardedMessage.writeInt(usernameLength);
            forwardedMessage.writeCharSequence(username, StandardCharsets.UTF_8);
            forwardedMessage.writeInt(publicKeySize);
            forwardedMessage.writeBytes(publicKeyBytes);
            server.broadcast(forwardedMessage);
        }
        else if (packetId == PacketID.MESSAGE.getId()) {
            int usernameLength = in.readInt();
            String username = in.readCharSequence(usernameLength, StandardCharsets.UTF_8).toString();
            int keyLength = in.readInt();
            byte[] keyBytes = new byte[keyLength];
            in.readBytes(keyBytes);
            int messageLength = in.readInt();
            byte[] messageBytes = new byte[messageLength];
            in.readBytes(messageBytes);

            String sender = "Unknown Sender";
            for (var entry : server.getChannels().entrySet()) {
                if (entry.getValue().equals(ctx.channel())) sender = entry.getKey();
            }

            LOGGER.info(String.format("Server recieved a message from %s for %s.", sender, username));

            ByteBuf msgBuf = Unpooled.buffer();
            msgBuf.writeShort(PacketID.MESSAGE.getId());
            msgBuf.writeInt(sender.length());
            msgBuf.writeCharSequence(sender, StandardCharsets.UTF_8);
            msgBuf.writeInt(keyLength);
            msgBuf.writeBytes(keyBytes);
            msgBuf.writeInt(messageLength);
            msgBuf.writeBytes(messageBytes);

            Channel ch = server.getChannels().get(username);
            if (ch == null) {
                LOGGER.warn(String.format("Channel not found for user %s.", username));
                return;
            }

            ch.writeAndFlush(msgBuf);
        }
        else if (packetId == PacketID.CATCHUP.getId()) {
            // Send Pubkey Catch-up
            ByteBuf pubKeyCatchupBuf = ctx.alloc().buffer();
            pubKeyCatchupBuf.writeShort(PacketID.CATCHUP.getId());
            pubKeyCatchupBuf.writeInt(server.getPublicKeys().size());
            for (Entry<String, PublicKey> entry : server.getPublicKeys().entrySet()) {
                pubKeyCatchupBuf.writeInt(entry.getKey().length());
                pubKeyCatchupBuf.writeCharSequence(entry.getKey(), StandardCharsets.UTF_8);
                pubKeyCatchupBuf.writeInt(entry.getValue().getEncoded().length);
                pubKeyCatchupBuf.writeBytes(entry.getValue().getEncoded());
            }
            ctx.writeAndFlush(pubKeyCatchupBuf);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        LOGGER.info("New connection registered.");
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        Map<String, Channel> channelsMap = server.getChannels();
        String username = null;

        for (Map.Entry<String, Channel> entry : channelsMap.entrySet()) {
            if (entry.getValue() == ctx.channel()) {
                username = entry.getKey();
                break;
            }
        }

        if (username != null) {
            server.getChannels().remove(username);
            server.getPublicKeys().remove(username);
            LOGGER.info(String.format("%s disconnected.", username));

            ByteBuf msg = Unpooled.buffer();
            msg.writeShort(PacketID.GOODBYE.getId());
            msg.writeInt(username.length());
            msg.writeCharSequence(username, StandardCharsets.UTF_8);

            server.broadcast(msg);
        }
        
        ctx.close();
    }
}
