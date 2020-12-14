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
public class PEStopSoundPacket extends DefinedPacket {

    public String name;
    public boolean stopAll;

    @Override
    public void read( ByteBuf buf ) {
        name = readString(buf);
        stopAll = buf.readBoolean();
    }

    @Override
    public void write( ByteBuf buf ) {
        writeString( name, buf );
        buf.writeBoolean( stopAll );
    }

    @Override
    public void handle( AbstractPacketHandler handler ) throws Exception {

    }
}