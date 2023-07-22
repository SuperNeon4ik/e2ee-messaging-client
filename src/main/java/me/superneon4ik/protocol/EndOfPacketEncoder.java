package me.superneon4ik.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class EndOfPacketEncoder extends MessageToByteEncoder<ByteBuf> {
    // Define your end-of-packet symbol here (e.g., a specific delimiter byte)
    private static final byte END_OF_PACKET_SYMBOL = (byte) 0xFF;

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) throws Exception {
        // Append the end-of-packet symbol to the outgoing data
        out.writeBytes(msg);
        out.writeByte(END_OF_PACKET_SYMBOL);
    }
}
