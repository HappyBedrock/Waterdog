package net.md_5.bungee.protocol.packet;

import io.netty.buffer.ByteBuf;
import java.util.UUID;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import net.md_5.bungee.protocol.AbstractPacketHandler;
import net.md_5.bungee.protocol.DefinedPacket;
import net.md_5.bungee.protocol.ProtocolConstants;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class BossBar extends DefinedPacket
{

    private UUID uuid;
    private int action;
    private String title;
    private float health;
    private int color;
    private int division;
    private byte flags;

    public BossBar(UUID uuid, int action)
    {
        this.uuid = uuid;
        this.action = action;
    }

    @Override
    public void read(ByteBuf buf, ProtocolConstants.Direction direction, int protocolVersion)
    {
        // Waterdog start
        if (ProtocolConstants.isPE(protocolVersion)) {
            uuid = new UUID(0, readSVarLong(buf));
            final int peAction = readVarInt(buf);
            try {
                title = "";
                health = 1.0f;
                color = 5;
                division = 0;
                switch (peAction) {
                    // Add
                    case 0:
                        action = 0;
                        title = readString(buf);
                        health = buf.readFloatLE();
                        buf.readShortLE(); // darken screen
                        color = readVarInt(buf);
                        division = readVarInt(buf);
                        break;
                    // Remove
                    case 2:
                        action = 1;
                        break;
                    // Player add
                    case 1:
                        // Player remove
                    case 3:
                        action = peAction == 1 ? 6 : 7;
                        readVarInt(buf); // player id
                        break;
                    // Health
                    case 4:
                        action = 2;
                        health = buf.readFloatLE();
                        break;
                    // Title
                    case 5:
                        action = 3;
                        title = readString(buf);
                        break;
                    // Flags
                    case 6:
                        action = 5;
                        buf.readShortLE(); // darken screen
                        color = readVarInt(buf);
                        division = readVarInt(buf);
                        break;
                    // Style
                    case 7:
                        action = 4;
                        color = readVarInt(buf);
                        division = readVarInt(buf);
                        break;
                }
            } catch (IndexOutOfBoundsException e) {} //Support truncated format too
            return;
        }
        // Waterdog end
        uuid = readUUID( buf );
        action = readVarInt( buf );

        switch ( action )
        {
            // Add
            case 0:
                title = readString( buf );
                health = buf.readFloat();
                color = readVarInt( buf );
                division = readVarInt( buf );
                flags = buf.readByte();
                break;
            // Health
            case 2:
                health = buf.readFloat();
                break;
            // Title
            case 3:
                title = readString( buf );
                break;
            // Style
            case 4:
                color = readVarInt( buf );
                division = readVarInt( buf );
                break;
            // Flags
            case 5:
                flags = buf.readByte();
                break;
        }
    }

    @Override
    public void write(ByteBuf buf, ProtocolConstants.Direction direction, int protocolVersion)
    {
        // Waterdog start
        if (ProtocolConstants.isPE(protocolVersion)) {
            writeSVarLong(uuid.getLeastSignificantBits(), buf);
            switch (action) {
                // Add
                case 0:
                    writeVarInt(0, buf);
                    writeString(title, buf);
                    buf.writeFloatLE(health);
                    buf.writeShortLE(12); // darken
                    writeVarInt(color, buf);
                    writeVarInt(division, buf);
                    break;
                // Remove
                case 1:
                    writeVarInt(2, buf);
                    break;
                // Health
                case 2:
                    writeVarInt(4, buf);
                    buf.writeFloatLE(health);
                    break;
                // Title
                case 3:
                    writeVarInt(5, buf);
                    writeString(title, buf);
                    break;
                // Style
                case 4:
                    writeVarInt(7, buf);
                    writeVarInt(color, buf);
                    writeVarInt(division, buf);
                    break;
                // Flags
                case 5:
                    writeVarInt(6, buf);
                    buf.writeShortLE(12); // darken
                    writeVarInt(color, buf);
                    writeVarInt(division, buf);
                    break;
                // Player add
                case 6:
                // Player remove
                case 7:
                    writeVarInt(action == 6 ? 1 : 3, buf);
                    writeVarInt(0, buf); // player id
                    break;
                default:
                    writeVarInt(action, buf);
            }
            return;
        }
        // Waterdog end
        writeUUID( uuid, buf );
        writeVarInt( action, buf );

        switch ( action )
        {
            // Add
            case 0:
                writeString( title, buf );
                buf.writeFloat( health );
                writeVarInt( color, buf );
                writeVarInt( division, buf );
                buf.writeByte( flags );
                break;
            // Health
            case 2:
                buf.writeFloat( health );
                break;
            // Title
            case 3:
                writeString( title, buf );
                break;
            // Style
            case 4:
                writeVarInt( color, buf );
                writeVarInt( division, buf );
                break;
            // Flags
            case 5:
                buf.writeByte( flags );
                break;
        }
    }

    @Override
    public void handle(AbstractPacketHandler handler) throws Exception
    {
        handler.handle( this );
    }
}
