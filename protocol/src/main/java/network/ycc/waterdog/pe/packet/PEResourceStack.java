package network.ycc.waterdog.pe.packet;

import net.md_5.bungee.protocol.ProtocolConstants;
import network.ycc.waterdog.pe.PEResourcePackData;

import io.netty.buffer.ByteBuf;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

import net.md_5.bungee.protocol.AbstractPacketHandler;
import net.md_5.bungee.protocol.DefinedPacket;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class PEResourceStack extends DefinedPacket {
    private boolean required = true;
    private boolean isExperimental = true;
    private PEResourcePackData[] resourcePacks = null;
    private PEResourcePackData[] behaviorPacks = null;
    private String baseGameVersion = "1.16.0";

    @Override
    public void read(ByteBuf buf) {
        buf.skipBytes(buf.readableBytes());
    }

    @Override
    public void write(ByteBuf buf, ProtocolConstants.Direction direction, int protocolVersion) {
        buf.writeBoolean(false);
        writePacks(buf, behaviorPacks);
        writePacks(buf, resourcePacks);
        if (protocolVersion < ProtocolConstants.MINECRAFT_PE_1_16_100){
            buf.writeBoolean(isExperimental);
        }
        DefinedPacket.writeString(baseGameVersion, buf);
        if (protocolVersion >= ProtocolConstants.MINECRAFT_PE_1_16_100){
            buf.writeIntLE(0); // Experiments length
            buf.writeBoolean(false); // Were experiments previously toggled
        }
    }

    @Override
    public void handle(AbstractPacketHandler handler) throws Exception {
        handler.handle(this);
    }

    protected void writePacks(ByteBuf buf, PEResourcePackData[] packs) {
        writeVarInt(packs == null ? 0 : packs.length, buf);
        if (packs != null) {
            for (PEResourcePackData pack : packs) {
                writeString(pack.getUuid().toString(), buf);
                writeString(pack.getVersion(), buf);
                writeString("", buf); //sub package name
            }
        }
    }
}
