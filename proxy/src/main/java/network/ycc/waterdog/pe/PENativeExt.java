package network.ycc.waterdog.pe;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;

import io.netty.channel.unix.Buffer;

import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;

import net.md_5.bungee.jni.cipher.BungeeCipher;
import net.md_5.bungee.jni.cipher.NativeCipher;
import net.md_5.bungee.jni.zlib.NativeZlib;

public class PENativeExt {
    public static final int CHUNK_SIZE = 8192;
    public static final int CHUNK_FLOOR = 512;

    public static CompositeByteBuf inflateZlibComposite(NativeZlib nzl, ByteBuf in, ByteBufAllocator alloc) {
        final CompositeByteBuf out = alloc.compositeDirectBuffer(128);
        final ByteBuffer[] buffers = in.nioBuffers();
        ByteBuf outChunk = alloc.directBuffer(CHUNK_SIZE, CHUNK_SIZE);
        int bufferIndex = 0;

        assert !nzl.isCompress();

        while ( !nzl.getFinished() && bufferIndex != buffers.length )
        {
            if (outChunk.writableBytes() < CHUNK_FLOOR) {
                out.addComponent(true, outChunk);
                outChunk = alloc.directBuffer(CHUNK_SIZE, CHUNK_SIZE);
            }

            final ByteBuffer inChunk = buffers[Math.min(bufferIndex, buffers.length - 1)];

            int processed = nzl.processEx(
                    Buffer.memoryAddress(inChunk) + inChunk.position(), inChunk.remaining(),
                    outChunk.memoryAddress() + outChunk.writerIndex(), outChunk.writableBytes());

            inChunk.position(inChunk.position() + nzl.getConsumed());
            outChunk.writerIndex(outChunk.writerIndex() + processed);

            if (!inChunk.hasRemaining()) {
                bufferIndex++;
            }
        }

        if (outChunk.isReadable()) {
            out.addComponent(true, outChunk);
        } else {
            outChunk.release();
        }

        nzl.doReset();

        return out;
    }

    public static void staticPEHash(NativeCipher cipher, long counter, ByteBuf in, ByteBuf key, ByteBuf out) {
        if (in.hasMemoryAddress()) {
            cipher.staticPEHash(counter, in, key, out);
        } else {
            final ByteBuffer[] buffers = in.nioBuffers();
            cipher.updateLongLE(counter);
            for (ByteBuffer buffer : buffers) {
                if (buffer.remaining() > 0) {
                    cipher.updateUnsafe(Buffer.memoryAddress(buffer) + buffer.position(), buffer.remaining());
                }
            }
            cipher.update(key.markReaderIndex());
            key.resetReaderIndex();
            cipher.digest(out);
        }
    }

    public static void cipherInPlace(NativeCipher cipher, ByteBuf buf) {
        if (buf.hasMemoryAddress()) {
            cipher.cipherUnsafe(buf.memoryAddress() + buf.readerIndex(), buf.readableBytes());
        } else {
            final ByteBuffer[] buffers = buf.nioBuffers();
            for (ByteBuffer buffer : buffers) {
                if (buffer.remaining() > 0) {
                    cipher.cipherUnsafe(Buffer.memoryAddress(buffer) + buffer.position(), buffer.remaining());
                }
            }
        }
    }

    public static void cipherInPlace(BungeeCipher cipher, ByteBuf buf) throws GeneralSecurityException {
        if (cipher instanceof NativeCipher) {
            cipherInPlace((NativeCipher) cipher, buf);
        } else {
            cipher.cipher(buf.duplicate(), buf.duplicate().writerIndex(buf.readerIndex()));
        }
    }
}
