package me.superneon4ik.protocol;

import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

public class EndOfPacketHandler extends ByteToMessageDecoder {
    // Define your end-of-packet symbol here (e.g., a specific delimiter byte)
    private static final byte END_OF_PACKET_SYMBOL = (byte) 0xFF;

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        // Search for the end-of-packet symbol in the incoming data
        int endIndex = in.indexOf(in.readerIndex(), in.writerIndex(), END_OF_PACKET_SYMBOL);

        if (endIndex != -1) {
            // Found the end-of-packet symbol, extract the complete packet
            ByteBuf packetBuf = in.readSlice(endIndex - in.readerIndex() + 1);
            packetBuf.retain();
            out.add(packetBuf);

            // Ensure there are enough bytes to skip before discarding the end-of-packet symbol
            int bytesToSkip = Math.min(1, in.readableBytes());
            in.skipBytes(bytesToSkip); // Skip the end-of-packet symbol
        }
    }
}
