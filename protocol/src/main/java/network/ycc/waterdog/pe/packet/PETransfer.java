package network.ycc.waterdog.pe.packet;

import io.netty.buffer.ByteBuf;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import net.md_5.bungee.protocol.AbstractPacketHandler;
import net.md_5.bungee.protocol.DefinedPacket;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class PETransfer extends DefinedPacket {
    String host;
    int port;

    @Override
    public void read(ByteBuf buf)
    {
        host = readString(buf);
        port = buf.readUnsignedShortLE();
    }

    @Override
    public void write(ByteBuf buf)
    {
        writeString(host, buf);
        buf.writeShortLE(port);
    }

    @Override
    public void handle(AbstractPacketHandler handler) throws Exception {
        //TODO: real handler to handle server drive transfer. should remember the server if we can
    }
}
