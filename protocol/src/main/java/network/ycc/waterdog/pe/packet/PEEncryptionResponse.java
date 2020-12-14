package network.ycc.waterdog.pe.packet;

import io.netty.buffer.ByteBuf;

import lombok.Data;
import lombok.NoArgsConstructor;

import net.md_5.bungee.protocol.AbstractPacketHandler;
import net.md_5.bungee.protocol.DefinedPacket;

@Data
@NoArgsConstructor
public class PEEncryptionResponse extends DefinedPacket {
    @Override
    public void write(ByteBuf buf) {

    }

    @Override
    public void read(ByteBuf buf) {
        buf.skipBytes(buf.readableBytes());
    }

    public void handle(AbstractPacketHandler handler) throws Exception {
        handler.handle(this);
    }
}
