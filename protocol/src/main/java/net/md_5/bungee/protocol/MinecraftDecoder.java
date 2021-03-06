package net.md_5.bungee.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.MessageToMessageDecoder;
import lombok.AllArgsConstructor;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
public class MinecraftDecoder extends MessageToMessageDecoder<ByteBuf>
{

    @Setter
    private Protocol protocol;
    private final boolean server;
    @Setter
    private int protocolVersion;
    @Setter
    private boolean supportsForge = false;

    public MinecraftDecoder(Protocol protocol, boolean server, int protocolVersion) {
        this.protocol = protocol;
        this.server = server;
        this.protocolVersion = protocolVersion;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        Protocol.DirectionData prot = (server) ? protocol.TO_SERVER : protocol.TO_CLIENT;
        // Waterdog start - replace in.copy()
        //ByteBuf slice = in.copy(); // Can't slice this one due to EntityMap :(
        /* Modification due to a misunderstanding of the primary (md_5) or secondary (yesdog) author... not sure.
        But lets make sure we have 16 bytes of *head* room for rewriting, plus 16 tail. */
        //TODO: lets figure out some way to make this not suck so much. maybe only resize/copy if the size really needs to change
        ByteBuf slice = ctx.alloc().ioBuffer(in.readableBytes() + 32);
        slice.writerIndex(slice.writerIndex() + 16);
        slice.readerIndex(slice.writerIndex());
        try {
            slice.writeBytes(in.markReaderIndex());
        } finally {
            in.resetReaderIndex();
        }
        // Waterdog end

        Object packetTypeInfo = null;
        try
        {
            // Waterfall start
            if (in.readableBytes() == 0) {
                return;
            }
            // Waterfall end

            int packetId = DefinedPacket.readVarInt( in );
            packetTypeInfo = packetId;

            DefinedPacket packet = prot.createPacket( packetId, protocolVersion, supportsForge );
            if ( packet != null )
            {
                packetTypeInfo = packet.getClass();
                packet.read0( in, prot.getDirection(), protocolVersion );

                if ( in.isReadable() )
                {
                    throw new BadPacketException( "Did not read all bytes from packet " + packet.getClass() + " " + packetId + " Protocol " + protocol + " Direction " + prot.getDirection() );
                }
            } else
            {
                in.skipBytes( in.readableBytes() );
            }

            out.add( new PacketWrapper( packet, slice ) );
            slice = null;
        } catch (BadPacketException | IndexOutOfBoundsException e) {
            final String packetTypeStr;
            if (packetTypeInfo instanceof Integer) {
                packetTypeStr = "id " + Integer.toHexString((Integer) packetTypeInfo);
            } else if (packetTypeInfo instanceof Class) {
                packetTypeStr = "class " + ((Class) packetTypeInfo).getSimpleName();
            } else {
                packetTypeStr = "unknown";
            }
            throw new FastDecoderException("Error decoding packet " + packetTypeStr + " with contents:\n" + ByteBufUtil.prettyHexDump(slice), e); // Waterfall
        } finally
        {
            if ( slice != null )
            {
                slice.release();
            }
        }
    }
}
