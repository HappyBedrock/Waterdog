package network.ycc.waterdog.pe;

import io.netty.buffer.ByteBuf;

import java.io.IOException;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public class PEZlib {

    private static final int level = 4;
    private static final ThreadLocal<Deflater> DEFLATER = ThreadLocal.withInitial(() -> new Deflater(level, false));
    private static final ThreadLocal<Deflater> DEFLATER_RAW = ThreadLocal.withInitial(() -> new Deflater(level, true));
    private static final ThreadLocal<Inflater> INFLATER_RAW = ThreadLocal.withInitial(() -> new Inflater(true));
    private static final ThreadLocal<Inflater> INFLATER = ThreadLocal.withInitial(() -> new Inflater(false));
    private static final ThreadLocal<byte[]> BUFFER = ThreadLocal.withInitial(() -> new byte[1024]);

    public void deflate(ByteBuf in, ByteBuf compressed, boolean rawDeflate){
        byte[] data = new byte[in.readableBytes()];
        byte[] buffer = BUFFER.get();
        in.readBytes(data);

        Deflater deflater = rawDeflate? DEFLATER_RAW.get() : DEFLATER.get();
        deflater.reset();
        deflater.setLevel(level);
        deflater.setInput(data);
        deflater.finish();

        while (!deflater.finished()) {
            int i = deflater.deflate(buffer);
            compressed.writeBytes(buffer, 0, i);
        }
    }

    public void inflate(ByteBuf in, ByteBuf inflated, boolean rawDeflate) throws IOException{
        byte[] data = new byte[in.readableBytes()];
        byte[] buffer = BUFFER.get();
        in.readBytes(data);

        Inflater inflater = rawDeflate? INFLATER_RAW.get() : INFLATER.get();
        inflater.reset();
        inflater.setInput(data);
        inflater.finished();

        try {
            while (!inflater.finished()) {
                int i = inflater.inflate(buffer);
                inflated.writeBytes(buffer, 0, i);
            }
        } catch (DataFormatException e) {
            throw new IOException("Unable to inflate zlib stream", e);
        }
    }

    public int getLevel() {
        return level;
    }
}
