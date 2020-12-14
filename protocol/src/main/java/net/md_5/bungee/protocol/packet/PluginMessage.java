package net.md_5.bungee.protocol.packet;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import io.github.waterfallmc.travertine.protocol.MultiVersionPacketV17;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil; // Waterfall
import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
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
public class PluginMessage extends MultiVersionPacketV17
{

    public static final Function<String, String> MODERNISE = new Function<String, String>()
    {
        @Override
        public String apply(String tag)
        {
            // Transform as per Bukkit
            if ( tag.equals( "BungeeCord" ) )
            {
                return "bungeecord:main";
            }
            if ( tag.equals( "bungeecord:main" ) )
            {
                return "BungeeCord";
            }

            // Code that gets to here is UNLIKELY to be viable on the Bukkit side of side things,
            // but we keep it anyway. It will eventually be enforced API side.
            if ( tag.indexOf( ':' ) != -1 )
            {
                return tag;
            }

            return "legacy:" + tag.toLowerCase( Locale.ROOT );
        }
    };
    public static final Predicate<PluginMessage> SHOULD_RELAY = new Predicate<PluginMessage>()
    {
        @Override
        public boolean apply(PluginMessage input)
        {
            return ( input.getTag().equals( "REGISTER" ) || input.getTag().equals( "minecraft:register" ) || input.getTag().equals( "MC|Brand" ) || input.getTag().equals( "minecraft:brand" ) ) && input.getData().length < Byte.MAX_VALUE;
        }
    };

    public PluginMessage(String tag, ByteBuf data, boolean allowExtendedPacket) {
        this(tag, ByteBufUtil.getBytes(data), allowExtendedPacket);
    }

    private String tag;
    private byte[] data;

    public void setData(byte[] data) {
        this.data = Preconditions.checkNotNull(data, "Null data");
    }

    public void setData(ByteBuf buf) {
        Preconditions.checkNotNull(buf, "Null buffer");
        setData(ByteBufUtil.getBytes(buf));
    }

    /**
     * Allow this packet to be sent as an "extended" packet.
     */
    private boolean allowExtendedPacket = false;

    // Travertine start
    @Override
    public void v17Read(ByteBuf buf, ProtocolConstants.Direction direction, int protocolVersion)
    {
        tag = readString( buf );
        data = v17readArray( buf );
    }
    // Travertine end

    @Override
    public void read(ByteBuf buf, ProtocolConstants.Direction direction, int protocolVersion)
    {
        tag = ( protocolVersion >= ProtocolConstants.MINECRAFT_1_13 || ProtocolConstants.isPE(protocolVersion) ) ? MODERNISE.apply( readString( buf ) ) : readString( buf ); // Waterdog
        if (ProtocolConstants.isPE(protocolVersion) && buf.isReadable()) buf = buf.readSlice(readVarInt(buf)); // Waterdog - varint slice
        int maxSize = direction == ProtocolConstants.Direction.TO_SERVER ? Short.MAX_VALUE : 0x100000;
        Preconditions.checkArgument( buf.readableBytes() < maxSize );
        data = new byte[ buf.readableBytes() ];
        buf.readBytes( data );
    }

    // Travertine start
    @Override
    public void v17Write(ByteBuf buf, ProtocolConstants.Direction direction, int protocolVersion)
    {
        writeString( tag, buf );
        v17writeArray( data, buf, allowExtendedPacket );
    }
    // Travertine end

    @Override
    public void write(ByteBuf buf, ProtocolConstants.Direction direction, int protocolVersion)
    {
        writeString( ( protocolVersion >= ProtocolConstants.MINECRAFT_1_13 || ProtocolConstants.isPE(protocolVersion) ) ? MODERNISE.apply( tag ) : tag, buf ); // Waterdog
        if (ProtocolConstants.isPE(protocolVersion)) writeVarInt(data.length, buf); // Waterdog - varint lengths
        buf.writeBytes( data );
    }

    @Override
    public void handle(AbstractPacketHandler handler) throws Exception
    {
        handler.handle( this );
    }

    public DataInput getStream()
    {
        return new DataInputStream( new ByteArrayInputStream( data ) );
    }
}
