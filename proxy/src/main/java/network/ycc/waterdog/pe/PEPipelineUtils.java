package network.ycc.waterdog.pe;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.AdaptiveRecvByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.channel.epoll.EpollDatagramChannel;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.unix.UnixChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.util.ReferenceCountUtil;

import lombok.RequiredArgsConstructor;

import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.UserConnection;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ListenerInfo;
import net.md_5.bungee.netty.PipelineUtils;
import net.md_5.bungee.protocol.MinecraftDecoder;
import net.md_5.bungee.protocol.ProtocolConstants;
import net.md_5.bungee.query.QueryHandler;

import network.ycc.raknet.pipeline.PongHandler;
import network.ycc.raknet.pipeline.UserDataCodec;

import network.ycc.waterdog.api.event.UserChannelTapEvent;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PEPipelineUtils {
    public static final UserDataCodec MC_USER_DATA_CODEC = new UserDataCodec(0xFE);

    private static final int SERVER_LOW_MARK = Integer.getInteger(
            "network.ycc.waterdog.server_low_mark",   5 * 1024 * 1024 ); // 5 mb
    private static final int SERVER_HIGH_MARK = Integer.getInteger(
            "network.ycc.waterdog.server_high_mark", 10 * 1024 * 1024 ); // 10 mb
    private static final WriteBufferWaterMark SERVER_MARK = new WriteBufferWaterMark( SERVER_LOW_MARK, SERVER_HIGH_MARK );

    private static final int REUSE_LISTENERS = Integer.getInteger(
            "network.ycc.waterdog.reuse_listeners", Runtime.getRuntime().availableProcessors());

    public static final ChannelInitializer<Channel> SERVER_CHILD = new ChannelInitializer<Channel>() {
        protected void initChannel(Channel channel) {
            ChannelPipeline pipeline = channel.pipeline();
            pipeline
            .addLast(UserDataCodec.NAME, MC_USER_DATA_CODEC)
            .addLast(PECompressor.NAME, new PECompressor())
            .addLast(PEDecompressor.NAME, new PEDecompressor());
            BungeeCord.getInstance().getPluginManager().callEvent(
                    new UserChannelTapEvent(pipeline.channel()));
            pipeline
            .addLast(PEDimSwitchLock.NAME, new PEDimSwitchLock())
            .addLast(new ChannelInboundHandlerAdapter() {
                @Override
                public void channelRead(ChannelHandlerContext ctx, Object msg) {
                    if (msg instanceof network.ycc.raknet.packet.Packet) {
                        BungeeCord.getInstance().getLogger().log(Level.FINER,
                                "Stray RakNet packet sent to a child channel handler");
                        ReferenceCountUtil.safeRelease(msg);
                        return;
                    }
                    ctx.fireChannelRead(msg);
                }
            })
            .addLast(PipelineUtils.SERVER_CHILD);
            pipeline.replace(PongHandler.NAME, PEPongHandler.NAME, PEPongHandler.INSTANCE);
            pipeline.replace(PipelineUtils.FRAME_DECODER, PipelineUtils.FRAME_DECODER, new ChannelInboundHandlerAdapter());
            pipeline.replace(PipelineUtils.FRAME_PREPENDER, PipelineUtils.FRAME_PREPENDER, new ChannelInboundHandlerAdapter());
            pipeline.get(MinecraftDecoder.class).setProtocolVersion(ProtocolConstants.MINECRAFT_PE_1_8);
            moveTimeout(channel);
        }
    };

    public static void enableDownstreamScatteredReads(Bootstrap bootstrap) {
        if (PipelineUtils.getDatagramChannel().equals(EpollDatagramChannel.class)) {
            bootstrap.option(EpollChannelOption.MAX_DATAGRAM_PAYLOAD_SIZE, 4 * 1024);
            bootstrap.option(ChannelOption.RCVBUF_ALLOCATOR, new AdaptiveRecvByteBufAllocator(
                    4 * 1024, 8 * 1024, 16 * 1024));
        }
    }

    public static void moveTimeout(Channel channel) {
        channel.eventLoop().execute(() -> {
            //move timeout to the top of the pipeline
            channel.pipeline().remove(PipelineUtils.TIMEOUT_HANDLER);
            channel.pipeline().addFirst(PipelineUtils.TIMEOUT_HANDLER,
                    new ReadTimeoutHandler( BungeeCord.getInstance().config.getTimeout(), TimeUnit.MILLISECONDS ));
        });
    }

    public static void multiServerBootstrap(ChannelFutureListener listener, Logger logger, Supplier<ServerBootstrap> f) {
        final ServerBootstrap bootstrap = f.get();
        if (PipelineUtils.getDatagramChannel().equals(EpollDatagramChannel.class)) {
            if (REUSE_LISTENERS > 1) {
                logger.info("Epoll enabled, creating " + REUSE_LISTENERS + " RakNet sockets with SO_REUSEPORT");
                bootstrap.option(UnixChannelOption.SO_REUSEPORT, true);
            }
            bootstrap.option(EpollChannelOption.MAX_DATAGRAM_PAYLOAD_SIZE, 4 * 1024);
            for (int i = 0 ; i < REUSE_LISTENERS ; i++) {
                bootstrap.clone()
                .option(ChannelOption.RCVBUF_ALLOCATOR, new AdaptiveRecvByteBufAllocator(
                        4 * 1024, 64 * 1024, 256 * 1024))
                .bind().addListener(listener);
            }
        } else {
            bootstrap.bind().addListener(listener);
        }
    }

    @RequiredArgsConstructor
    public static final class ServerChannel extends ChannelInitializer<Channel> {
        private final ProxyServer bungee;
        private final ListenerInfo listener;

        @Override
        protected void initChannel(Channel channel) throws Exception {
            channel.eventLoop().execute(() -> {
                channel.pipeline().addLast(new ChannelInboundHandlerAdapter() {
                    @Override
                    public void channelRead(ChannelHandlerContext ctx, Object msg) {
                        if (msg instanceof DatagramPacket) {
                            bungee.getLogger().log(Level.FINER, "Stray datagram sent to server channel handler");
                            ReferenceCountUtil.safeRelease(msg);
                            return;
                        }
                        ctx.fireChannelRead(msg);
                    }
                });
            });
            channel.config().setWriteBufferWaterMark(SERVER_MARK);
            channel.pipeline()
            .addLast(new PEProxyServerInfoHandler(bungee, listener))
            .addLast(new QueryHandler(bungee, listener));
        }
    }
}
