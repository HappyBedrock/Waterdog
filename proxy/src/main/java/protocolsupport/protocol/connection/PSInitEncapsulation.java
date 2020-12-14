package protocolsupport.protocol.connection;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import lombok.AllArgsConstructor;

import net.md_5.bungee.protocol.DefinedPacket;

import java.net.InetSocketAddress;

@AllArgsConstructor
public class PSInitEncapsulation extends ChannelInboundHandlerAdapter {
    public static final int HANDSHAKE_ID = 0;
    public static final int CURRENT_VERSION = 1;

    private final InetSocketAddress address;
    private final boolean compression;

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        final ByteBuf to = ctx.alloc().heapBuffer();
        to.writeByte(HANDSHAKE_ID);
        DefinedPacket.writeVarInt(CURRENT_VERSION, to);
        if (address != null) {
            final byte[] addr = address.getAddress().getAddress();
            to.writeBoolean(true);
            DefinedPacket.writeVarInt(addr.length, to);
            to.writeBytes(addr);
            DefinedPacket.writeVarInt(address.getPort(), to);
        } else {
            to.writeBoolean(false);
        }
        to.writeBoolean(compression);
        ctx.writeAndFlush(to);
        ctx.fireChannelActive();
        ctx.pipeline().remove(this);
    }
}
