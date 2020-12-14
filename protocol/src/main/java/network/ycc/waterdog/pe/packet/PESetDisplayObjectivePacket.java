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
public class PESetDisplayObjectivePacket extends DefinedPacket {
    private String displaySlot;
    private String objectiveName;
    private String displayName;
    private String criteriaName;
    private int sortOrder;

    @Override
    public void write(ByteBuf buf) {
        writeString(displaySlot, buf);
        writeString(objectiveName, buf);
        writeString(displayName, buf);
        writeString(criteriaName, buf);
        buf.writeByte(sortOrder);
    }

    @Override
    public void read(ByteBuf buf) {
        displaySlot = readString(buf);
        objectiveName = readString(buf);
        displayName = readString(buf);
        criteriaName = readString(buf);
        sortOrder = buf.readUnsignedByte();
    }

    @Override
    public void handle(AbstractPacketHandler handler) throws Exception {
        handler.handle(this);
    }
}
