package network.ycc.waterdog.pe.packet;

import io.netty.buffer.ByteBuf;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import net.md_5.bungee.protocol.AbstractPacketHandler;
import net.md_5.bungee.protocol.DefinedPacket;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PEEncryptionRequest extends DefinedPacket {
    private String jwtData;

    @Override
    public void read(ByteBuf buf) {
        jwtData = readString(buf);
    }

    @Override
    public void write(ByteBuf buf) {
        writeString(jwtData, buf);
    }

    public void handle(AbstractPacketHandler handler) throws Exception {
        handler.handle(this);
    }
}
