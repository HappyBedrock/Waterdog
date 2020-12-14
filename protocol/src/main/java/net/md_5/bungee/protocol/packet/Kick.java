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
public class Kick extends DefinedPacket
{

    private String message;

    @Override
    public void read(ByteBuf buf, net.md_5.bungee.protocol.ProtocolConstants.Direction direction, int protocolVersion) // Waterdog
    {
        // Waterdog start
        if (net.md_5.bungee.protocol.ProtocolConstants.isPE(protocolVersion)) {
            buf.readBoolean(); //hide disconnect screen
            message = net.md_5.bungee.chat.ComponentSerializer.toString(new net.md_5.bungee.api.chat.TextComponent(readString(buf)));
            return;
        }
        // Waterdog end
        message = readString( buf );
    }

    @Override
    public void write(ByteBuf buf, net.md_5.bungee.protocol.ProtocolConstants.Direction direction, int protocolVersion) // Waterdog
    {
        // Waterdog start
        if (net.md_5.bungee.protocol.ProtocolConstants.isPE(protocolVersion)) {
            buf.writeBoolean( false ); // hide disconnect screen
            String disconnectText = net.md_5.bungee.chat.ComponentSerializer.parse(message)[0].toLegacyText();
            if (disconnectText.startsWith("§f")) {
                disconnectText = disconnectText.substring(2);
            }
            writeString(disconnectText, buf);
            return;
        }
        // Waterdog end
        writeString( message, buf );
    }

    @Override
    public void handle(AbstractPacketHandler handler) throws Exception
    {
        handler.handle( this );
    }
}
