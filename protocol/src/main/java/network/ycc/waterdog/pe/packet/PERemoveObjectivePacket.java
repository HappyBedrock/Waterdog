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
public class PERemoveObjectivePacket extends DefinedPacket {
    private String objectiveName;

    @Override
    public void write(ByteBuf buf) {
        writeString(objectiveName, buf);
    }

    @Override
    public void read(ByteBuf buf) {
        objectiveName = readString(buf);
    }

    @Override
    public void handle(AbstractPacketHandler handler) throws Exception {
        handler.handle(this);
    }
}
