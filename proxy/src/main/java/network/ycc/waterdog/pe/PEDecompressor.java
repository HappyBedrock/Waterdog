package network.ycc.waterdog.pe;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.ReferenceCountUtil;

import net.md_5.bungee.compress.PacketDecompressor;
import net.md_5.bungee.protocol.DefinedPacket;
import network.ycc.raknet.RakNet;

import java.util.List;

public class PEDecompressor extends PacketDecompressor {

    public static final String NAME = "decompress";
    private PEZlib zlib;

    public PEDecompressor() {
        super(0);
        zlib = new PEZlib();
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf buf, List<Object> out) throws Exception {
        ByteBuf data = ctx.alloc().ioBuffer(buf.readableBytes() * 4);
        boolean raw = RakNet.config(ctx).getProtocolVersion() == 10;
        try {
            zlib.inflate(buf, data, raw);
            while (data.isReadable()) {
                out.add(data.readRetainedSlice(DefinedPacket.readVarInt(data)));
            }
        } finally {
            ReferenceCountUtil.safeRelease(data);
        }
    }

}
