package net.md_5.bungee.protocol.packet;

import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import net.md_5.bungee.protocol.AbstractPacketHandler;
import net.md_5.bungee.protocol.DefinedPacket;
import net.md_5.bungee.protocol.ProtocolConstants;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class Respawn extends DefinedPacket
{

    private int dimension;
    private long seed;
    private short difficulty;
    private short gameMode;
    private String levelType;

    @Override
    public void read(ByteBuf buf, ProtocolConstants.Direction direction, int protocolVersion)
    {
        // Waterdog start
        if (net.md_5.bungee.protocol.ProtocolConstants.isPE(protocolVersion)) {
            dimension = network.ycc.waterdog.pe.PEDataValues.getPcDimensionId(readSVarInt(buf));
            buf.readFloatLE(); //x
            buf.readFloatLE(); //y
            buf.readFloatLE(); //z
            buf.readBoolean(); //respawn
            return;
        }
        // Waterdog end
        dimension = buf.readInt();
        if ( protocolVersion >= ProtocolConstants.MINECRAFT_1_15 )
        {
            seed = buf.readLong();
        }
        if ( protocolVersion < ProtocolConstants.MINECRAFT_1_14 )
        {
            difficulty = buf.readUnsignedByte();
        }
        gameMode = buf.readUnsignedByte();
        levelType = readString( buf );
    }

    @Override
    public void write(ByteBuf buf, ProtocolConstants.Direction direction, int protocolVersion)
    {
        // Waterdog start
        if (net.md_5.bungee.protocol.ProtocolConstants.isPE(protocolVersion)) {
            writeSVarInt(network.ycc.waterdog.pe.PEDataValues.getPeDimensionId(dimension), buf);
            buf.writeFloatLE(0); //x
            buf.writeFloatLE(300); //y
            buf.writeFloatLE(0); //z
            buf.writeBoolean(true); //respawn
            return;
        }
        // Waterdog end
        buf.writeInt( dimension );
        if ( protocolVersion >= ProtocolConstants.MINECRAFT_1_15 )
        {
            buf.writeLong( seed );
        }
        if ( protocolVersion < ProtocolConstants.MINECRAFT_1_14 )
        {
            buf.writeByte( difficulty );
        }
        buf.writeByte( gameMode );
        writeString( levelType, buf );
    }

    @Override
    public void handle(AbstractPacketHandler handler) throws Exception
    {
        handler.handle( this );
    }
}
