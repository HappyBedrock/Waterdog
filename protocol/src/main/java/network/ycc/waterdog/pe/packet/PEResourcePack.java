package network.ycc.waterdog.pe.packet;

import network.ycc.waterdog.pe.PEResourcePackData;

import io.netty.buffer.ByteBuf;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

import net.md_5.bungee.protocol.AbstractPacketHandler;
import net.md_5.bungee.protocol.DefinedPacket;
import net.md_5.bungee.protocol.ProtocolConstants;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class PEResourcePack extends DefinedPacket {
    private boolean required = true;
    private PEResourcePackData[] resourcePacks = new PEResourcePackData[0];
    private PEResourcePackData[] behaviorPacks = new PEResourcePackData[0];

    @Override
    public void read(ByteBuf buf) {
        buf.skipBytes(buf.readableBytes());
    }

    @Override
    public void write(ByteBuf buf, ProtocolConstants.Direction direction, int protocolVersion) {
        buf.writeBoolean(this.required);
        buf.writeBoolean(false); // Has scripts
        this.writePacks(buf, this.behaviorPacks, protocolVersion);
        this.writeResourcePacks(buf, this.resourcePacks, protocolVersion);
    }

    @Override
    public void handle(AbstractPacketHandler handler) throws Exception {
        handler.handle(this);
    }

    protected void writePacks(ByteBuf buf, PEResourcePackData[] packs, int protocolVersion) {
        buf.writeShortLE(packs.length);
        for (PEResourcePackData pack : packs) {
            this.writePackEntry(buf, pack, protocolVersion);
        }
    }

    protected void writeResourcePacks(ByteBuf buf, PEResourcePackData[] packs, int protocolVersion){
        buf.writeShortLE(packs.length);
        for (PEResourcePackData pack : packs) {
            this.writePackEntry(buf, pack, protocolVersion);
            if (protocolVersion >= ProtocolConstants.MINECRAFT_PE_1_16_200){
                buf.writeBoolean(false); // Is raytracing capable
            }
        }
    }

    private void writePackEntry(ByteBuf buf, PEResourcePackData pack, int protocolVersion) {
        DefinedPacket.writeString(pack.getUuid().toString(), buf); // PackId
        DefinedPacket.writeString(pack.getVersion(), buf); // Pack version
        buf.writeLongLE(pack.getSize()); // Pack size
        DefinedPacket.writeString("", buf); // Content key
        DefinedPacket.writeString("", buf); // Sub-pack name
        DefinedPacket.writeString("", buf); // Content id
        if (protocolVersion >= ProtocolConstants.MINECRAFT_PE_1_9) {
            buf.writeBoolean(false); // Has scripts
        }
    }
}
