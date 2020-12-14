package net.md_5.bungee.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.AllArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
public class MinecraftEncoder extends MessageToByteEncoder<DefinedPacket>
{

    @Setter
    private Protocol protocol;
    private boolean server;
    @Setter
    private int protocolVersion;

    @Override
    protected void encode(ChannelHandlerContext ctx, DefinedPacket msg, ByteBuf out) throws Exception
    {
        Protocol.DirectionData prot = ( server ) ? protocol.TO_CLIENT : protocol.TO_SERVER;
        DefinedPacket.writeVarInt( prot.getId( msg.getClass(), protocolVersion ), out );
        msg.write0( out, prot.getDirection(), protocolVersion );
    }

    // Waterdog start
    @Override
    public boolean acceptOutboundMessage(Object msg) throws Exception {
        Protocol.DirectionData prot = ( server ) ? protocol.TO_CLIENT : protocol.TO_SERVER;
        if (ProtocolConstants.isPE(protocolVersion) && msg instanceof DefinedPacket &&
                !prot.hasPacket(((DefinedPacket) msg).getClass(), protocolVersion)) return false;
        return super.acceptOutboundMessage(msg);
    }
    // Waterdog end
}
