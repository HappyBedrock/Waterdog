package net.md_5.bungee.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.CorruptedFrameException;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class Varint21FrameDecoder extends ByteToMessageDecoder
{

    private AtomicLong lastEmptyPacket = new AtomicLong(0); // Travertine
    private static boolean DIRECT_WARNING;

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception
    {
        in.markReaderIndex();

        for ( int i = 0; i < 3; i++ ) // Waterfall
        {
            if ( !in.isReadable() )
            {
                in.resetReaderIndex();
                return;
            }

            // Waterfall start
            byte read = in.readByte();
            if ( read >= 0 )
            {
                in.resetReaderIndex();
                int length = DefinedPacket.readVarInt( in );
                // Waterfall end
                if ( false && length == 0) // Waterfall - ignore
                {
                    // Travertine start - vanilla 1.7 client sometimes sends empty packets.
                    long currentTime = System.currentTimeMillis();
                    long lastEmptyPacket = this.lastEmptyPacket.getAndSet(currentTime);

                    if (currentTime - lastEmptyPacket < 50L)
                    {
                        throw new CorruptedFrameException( "Too many empty packets" );
                    }
                    // Travertine end
                }

                if ( in.readableBytes() < length )
                {
                    in.resetReaderIndex();
                    return;
                    // Waterfall start
                } else {
                    out.add(in.readRetainedSlice(length));
                    return;
                    // Waterfall end
                }
            }
        }

        throw new CorruptedFrameException( "length wider than 21-bit" );
    }
}
