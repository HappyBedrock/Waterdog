package network.ycc.waterdog.pe;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.ScheduledFuture;

import net.md_5.bungee.BungeeCord;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

/**
 * Lock outbound packet stream until we get a dim switch ack.
 */
public class PEDimSwitchLock extends ChannelDuplexHandler {

    public static final String NAME = "peproxy-dimlock";

    protected static int MAX_QUEUE_SIZE = 8192;
    protected static int DIM_ACK_TIMEOUT_MILLIS = 15000;

    protected final Queue<ByteBuf> queue = new ArrayDeque<>(32);
    protected boolean isLocked = false;
    protected ScheduledFuture<?> timeoutTimer = null;

    public static boolean isChannelLocked(Channel channel) {
        final PEDimSwitchLock thiz = (PEDimSwitchLock) channel.pipeline().get(NAME);
        return thiz != null ? thiz.isLocked : false;
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        super.channelUnregistered(ctx);
        queue.forEach(ReferenceCountUtil::safeRelease);
        queue.clear();
        stopTimeout();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof ByteBuf) {
            if(isLocked && PERawPacketData.isDimSwitchAck((ByteBuf) msg)) {
                doUnlock(ctx);
            }
        }
        ctx.fireChannelRead(msg);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
        if (msg instanceof ByteBuf) {
            if (isLocked) {
                queue.add((ByteBuf) msg);
                promise.trySuccess();
                if (queue.size() > MAX_QUEUE_SIZE) {
                    BungeeCord.getInstance().getLogger().warning(
                            "PEDimSwitchLock: queue got too large, closing connection.");
                    ctx.channel().close();
                }
                return;
            } else if (PERawPacketData.peekPacketId((ByteBuf) msg)
                    == PERawPacketData.EXT_PS_AWAIT_DIM_SWITCH_ACK_ID) {
                isLocked = true;
                startTimeout(ctx);
                ctx.fireChannelWritabilityChanged(); //alert UpstreamBridge
                return;
            }
        }
        ctx.write(msg, promise);
    }

    protected void startTimeout(ChannelHandlerContext ctx) {
        stopTimeout();
        timeoutTimer = ctx.channel().eventLoop().schedule(() -> {
            BungeeCord.getInstance().getLogger().warning(
                    "PEDimSwitchLock: Dim switch ack timeout. Resuming...");
            doUnlock(ctx);
        }, DIM_ACK_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
    }

    protected void stopTimeout() {
        if (timeoutTimer != null) {
            timeoutTimer.cancel(false);
            timeoutTimer = null;
        }
    }

    protected void doUnlock(ChannelHandlerContext ctx) {
        isLocked = false;
        stopTimeout();
        ctx.fireChannelWritabilityChanged(); //alert UpstreamBridge
        while (!queue.isEmpty() && !isLocked) {
            write(ctx, queue.remove(), ctx.voidPromise());
        }
        ctx.flush();
    }

}
