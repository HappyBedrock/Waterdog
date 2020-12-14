package net.md_5.bungee.protocol.packet;

import io.github.waterfallmc.travertine.protocol.MultiVersionPacketV17;
import io.netty.buffer.ByteBuf;
import java.util.Locale;
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
public class ScoreboardObjective extends MultiVersionPacketV17
{

    private String name;
    private String value;
    private HealthDisplay type;
    /**
     * 0 to create, 1 to remove, 2 to update display text.
     */
    private byte action;

    // Travertine start
    @Override
    public void v17Read(ByteBuf buf, ProtocolConstants.Direction direction, int protocolVersion)
    {
        name = readString( buf );
        value = readString( buf );
        action = buf.readByte();
    }
    // Travertine end

    @Override
    public void read(ByteBuf buf, ProtocolConstants.Direction direction, int protocolVersion)
    {
        name = readString( buf );
        action = buf.readByte();
        if ( action == 0 || action == 2 )
        {
            value = readString( buf );
            if ( protocolVersion >= ProtocolConstants.MINECRAFT_1_13 )
            {
                type = HealthDisplay.values()[readVarInt( buf )];
            } else
            {
                type = HealthDisplay.fromString( readString( buf ) );
            }
        }
    }

    // Travertine start
    @Override
    public void v17Write(ByteBuf buf, ProtocolConstants.Direction direction, int protocolVersion)
    {
        writeString( name, buf );
        writeString( value, buf );
        buf.writeByte( action );
    }
    // Travertine end

    @Override
    public void write(ByteBuf buf, ProtocolConstants.Direction direction, int protocolVersion)
    {
        writeString( name, buf );
        buf.writeByte( action );
        if ( action == 0 || action == 2 )
        {
            writeString( value, buf );
            if ( protocolVersion >= ProtocolConstants.MINECRAFT_1_13 )
            {
                writeVarInt( type.ordinal(), buf );
            } else
            {
                writeString( type.toString(), buf );
            }
        }
    }

    @Override
    public void handle(AbstractPacketHandler handler) throws Exception
    {
        handler.handle( this );
    }

    public enum HealthDisplay
    {

        INTEGER, HEARTS;

        @Override
        public String toString()
        {
            return super.toString().toLowerCase( Locale.ROOT );
        }

        public static HealthDisplay fromString(String s)
        {
            return valueOf( s.toUpperCase( Locale.ROOT ) );
        }
    }
}
