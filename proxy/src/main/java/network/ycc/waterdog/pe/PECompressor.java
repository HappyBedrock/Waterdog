package network.ycc.waterdog.pe;

import static network.ycc.waterdog.pe.PENativeExt.CHUNK_SIZE;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.util.ReferenceCountUtil;
import net.md_5.bungee.protocol.DefinedPacket;

import network.ycc.raknet.RakNet;
import network.ycc.raknet.pipeline.FlushTickHandler;
import network.ycc.waterdog.api.metrics.RakNetMetrics;

import java.io.IOException;
import java.util.function.BiConsumer;

public class PECompressor extends ChannelOutboundHandlerAdapter {

    public static final String NAME = "compress";

    protected static final int MAX_POOL_BYTES = 128 * 1024;
    protected static final int MAX_COMPONENTS = 512;

    protected final PEZlib zlib = new PEZlib();
    protected boolean dirty = false;
    protected CompositeByteBuf outBuffer, inBuffer;
    protected ByteBuf headerTmp, outTmp, inTmp;

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        outBuffer = ctx.alloc().compositeDirectBuffer(MAX_COMPONENTS);
        inBuffer = ctx.alloc().compositeDirectBuffer(MAX_COMPONENTS);
        headerTmp = ctx.alloc().directBuffer(8, 8);
        outTmp = ctx.alloc().directBuffer(CHUNK_SIZE, CHUNK_SIZE);
        inTmp = ctx.alloc().directBuffer(CHUNK_SIZE, CHUNK_SIZE);
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) {
        ReferenceCountUtil.safeRelease(outBuffer);
        ReferenceCountUtil.safeRelease(inBuffer);
        ReferenceCountUtil.safeRelease(headerTmp);
        ReferenceCountUtil.safeRelease(outTmp);
        ReferenceCountUtil.safeRelease(inTmp);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (!(msg instanceof ByteBuf)) {
            ctx.write(msg, promise);
            return;
        }

        final ByteBuf buf = (ByteBuf) msg;
        try {
            promise.trySuccess();
            if (!buf.isReadable()){
                return;
            }

            if (inBuffer.readableBytes() > MAX_POOL_BYTES) {
                flushData(ctx);
            }

            metricsIncrement(ctx, 4 + buf.readableBytes(), RakNetMetrics::preCompressionBytes);

            final ByteBuf header = ctx.alloc().directBuffer(8, 8);
            DefinedPacket.writeVarInt(buf.readableBytes(), header);
            inBuffer.addComponent(true, header);
            inBuffer.addComponent(true, buf.retain());

            dirty = true;
            metricsIncrement(ctx, 1, RakNetMetrics::preCompressionPacket);
            FlushTickHandler.checkFlushTick(ctx.channel());
        } finally {
            ReferenceCountUtil.safeRelease(msg);
        }
    }

    @Override
    public void flush(ChannelHandlerContext ctx) throws Exception {
        if (dirty) {
            flushData(ctx);
        }
        ctx.flush();
    }

    protected void flushData(ChannelHandlerContext ctx) throws IOException {
        dirty = false;

        final ByteBuf out = ctx.alloc().directBuffer(inBuffer.readableBytes() / 4 + 16);
        metricsIncrement(ctx, inBuffer.readableBytes(), RakNetMetrics::preCompressionBytes);

        boolean raw = false;
        if (ctx.channel().config() instanceof network.ycc.raknet.RakNet.Config) {
            RakNet.Config config = (RakNet.Config) ctx.channel().config();
            raw = config.getProtocolVersion() == 10;
        }

        zlib.deflate(inBuffer, out, raw);
        inBuffer.release();
        inBuffer = ctx.alloc().compositeDirectBuffer(MAX_COMPONENTS);

        metricsIncrement(ctx, out.readableBytes(), RakNetMetrics::postCompressionBytes);
        ctx.write(out).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
    }

    protected void metricsIncrement(ChannelHandlerContext ctx, int i, BiConsumer<RakNetMetrics, Integer> consumer) {
        final RakNet.MetricsLogger metrics = ctx.channel().config().getOption(RakNet.METRICS);
        if (metrics instanceof RakNetMetrics) {
            consumer.accept((RakNetMetrics) metrics, i);
        }
    }
}
