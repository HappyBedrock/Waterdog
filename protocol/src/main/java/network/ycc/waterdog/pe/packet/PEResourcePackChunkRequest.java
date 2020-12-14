package network.ycc.waterdog.pe.packet;

import io.netty.buffer.ByteBuf;

import java.util.UUID;

import lombok.Data;
import lombok.EqualsAndHashCode;

import net.md_5.bungee.protocol.AbstractPacketHandler;
import net.md_5.bungee.protocol.DefinedPacket;

@Data
@EqualsAndHashCode(callSuper = false)
public class PEResourcePackChunkRequest extends DefinedPacket {
    private UUID packUUID;
    private int chunkIndex;

    @Override
    public void read(ByteBuf buf) {
        packUUID = UUID.fromString(readString(buf));
        chunkIndex = buf.readIntLE();
    }

    public void handle(AbstractPacketHandler handler) throws Exception {
        handler.handle(this);
    }
}
