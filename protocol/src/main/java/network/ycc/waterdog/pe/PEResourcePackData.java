package network.ycc.waterdog.pe;

import io.netty.buffer.ByteBuf;

import java.util.UUID;

public interface PEResourcePackData {
    int CHUNK_SIZE = 1024 * 1024;

    void writeChunk(int chunkIndex, ByteBuf to, int version);
    String getName();
    UUID getUuid();
    String getVersion();
    int getSize();
    byte[] getSha256();

    default int getNumberChunks() {
        return (getSize() + CHUNK_SIZE - 1) / CHUNK_SIZE;
    }
}
