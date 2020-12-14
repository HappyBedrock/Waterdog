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
public class PELocalPlayerInitialized extends DefinedPacket {
    private long playerId;

    @Override
    public void read(ByteBuf buf) {
        playerId = readVarLong(buf);
    }

    @Override
    public void write(ByteBuf buf) {
        writeVarLong(playerId, buf);
    }

    @Override
    public void handle(AbstractPacketHandler handler) throws Exception {

    }
}
