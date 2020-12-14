package net.md_5.bungee.protocol.packet;

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
public class LoginSuccess extends DefinedPacket
{
    // Waterdog start
    public static final int PE_LOGIN_SUCCESS = 0;
    public static final int PE_LOGIN_FAILED_CLIENT = 1;
    public static final int PE_LOGIN_FAILED_SERVER = 2;
    public static final int PE_PLAYER_SPAWN = 3;

    private int statusCode = PE_LOGIN_SUCCESS;

    public LoginSuccess(String uuid, String username) {
        this.uuid = uuid;
        this.username = username;
    }
    // Waterdog end

    private String uuid;
    private String username;

    @Override
    public void read(ByteBuf buf, net.md_5.bungee.protocol.ProtocolConstants.Direction direction, int protocolVersion) // Waterdog
    {
        // Waterdog start
        if (net.md_5.bungee.protocol.ProtocolConstants.isPE(protocolVersion)) {
            statusCode = buf.readInt();
            return;
        }
        // Waterdog end
        uuid = readString(buf);
        username = readString(buf);
    }

    @Override
    public void write(ByteBuf buf, net.md_5.bungee.protocol.ProtocolConstants.Direction direction, int protocolVersion) // Waterdog
    {
        // Waterdog start
        if (net.md_5.bungee.protocol.ProtocolConstants.isPE(protocolVersion)) {
            buf.writeInt(statusCode);
            return;
        }
        // Waterdog end
        writeString(uuid, buf);
        writeString(username, buf);
    }

    @Override
    public void handle(AbstractPacketHandler handler) throws Exception
    {
        handler.handle( this );
    }
}
