package network.ycc.waterdog.pe;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;

import net.md_5.bungee.protocol.PacketWrapper;
import net.md_5.bungee.protocol.packet.KeepAlive;

import network.ycc.raknet.RakNet;
import network.ycc.raknet.packet.Pong;
import network.ycc.raknet.pipeline.PongHandler;

@ChannelHandler.Sharable
public class PEPongHandler extends PongHandler {
    public static final PEPongHandler INSTANCE = new PEPongHandler();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Pong pong) {
        super.channelRead0(ctx, pong);
        final RakNet.Config config = (RakNet.Config) ctx.channel().config();
        ctx.fireChannelRead(new PacketWrapper(new KeepAlive(config.getRTTNanos()), Unpooled.EMPTY_BUFFER));
    }
}
