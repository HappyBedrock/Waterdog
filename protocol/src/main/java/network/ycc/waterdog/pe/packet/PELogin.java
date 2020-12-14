package network.ycc.waterdog.pe.packet;

import com.nukkitx.nbt.NbtUtils;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import net.md_5.bungee.protocol.DefinedPacket;
import net.md_5.bungee.protocol.ProtocolConstants;
import net.md_5.bungee.protocol.packet.Login;

import network.ycc.waterdog.pe.PEDataValues;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class PELogin extends Login {
    private float spawnX;
    private float spawnY;
    private float spawnZ;
    private byte[] tailPayload;
    private int payloadPaletteOffset; // Offset of tailPayload for the palette
    private int payloadPaletteLength;
    private Map<String, Object> rules = new HashMap<>();

    public PELogin(int entityId, short gameMode, int dimension, short difficulty, short maxPlayers, String levelType, int viewDistance, boolean reducedDebugInfo, byte[] tailPayload) {
        super(entityId, gameMode, dimension, 0, difficulty, maxPlayers, levelType, viewDistance, reducedDebugInfo, true);
        this.tailPayload = tailPayload;
    }

    @Override
    public void read(ByteBuf buf, ProtocolConstants.Direction direction, int protocolVersion) {
        readSVarLong(buf); // Entity id (but it's actually signed varlong, so we use the field below, which is unsigned)
        setEntityId((int) readVarLong(buf));
        setGameMode((short) readSVarInt(buf));
        spawnX = buf.readFloatLE(); // X
        spawnY = buf.readFloatLE(); // Y
        spawnZ = buf.readFloatLE(); // Z
        buf.readFloatLE(); // Yaw
        buf.readFloatLE(); // Pitch
        readSVarInt(buf); // Seed
        if (protocolVersion >= ProtocolConstants.MINECRAFT_PE_1_16){
            buf.readShort(); // Type
            DefinedPacket.readString(buf); // Biome
        }
        setDimension(PEDataValues.getPcDimensionId(readSVarInt(buf)));
        readSVarInt(buf); // World type (1 - infinite)
        readSVarInt(buf); // World gamemode (SURVIVAL)
        setDifficulty((short) readSVarInt(buf));

        final int payloadStartIndex = buf.readerIndex();
        tailPayload = new byte[buf.readableBytes()];
        buf.readBytes(tailPayload);
        buf.readerIndex(payloadStartIndex);

        DefinedPacket.readSVarInt(buf); // Spawn position
        DefinedPacket.readSVarLong(buf); // Should be VarInt, but nukkit... //TODO: wtf
        DefinedPacket.readSVarInt(buf);
        buf.readBoolean(); // Disable achievements
        DefinedPacket.readSVarInt(buf); // Time
        DefinedPacket.readSVarInt(buf); // Edu mode
        buf.readBoolean(); // Edu features
        if (protocolVersion >= ProtocolConstants.MINECRAFT_PE_1_16) {
            DefinedPacket.readString(buf); // educationProductId
        }
        buf.readFloatLE(); // Rain level
        buf.readFloatLE(); // Lighting level
        if (protocolVersion >= ProtocolConstants.MINECRAFT_PE_1_9) {
            buf.readBoolean(); // Has confirmed platform locked content
        }
        buf.readBoolean(); // Is multiplayer
        buf.readBoolean(); // Broadcast to lan
        if (protocolVersion >= ProtocolConstants.MINECRAFT_PE_1_9) {
            DefinedPacket.readSVarInt(buf); // Xbox live broadcast, 3 = friends of friends
            DefinedPacket.readSVarInt(buf); // Platform broadcast
        } else {
            buf.readBoolean(); // Broadcast to xbl
        }
        buf.readBoolean(); // Commands enabled
        buf.readBoolean(); // Needs texture pack

        final int nGameRules = DefinedPacket.readVarInt(buf); // Game rules
        for (int i = 0 ; i < nGameRules ; i++) {
            String rule = DefinedPacket.readString(buf);
            Object value;

            final int grType = DefinedPacket.readVarInt(buf);
            switch (grType) {
                case 1: {
                    value = buf.readByte();
                    break;
                }
                case 2: {
                    value = DefinedPacket.readSVarInt(buf);
                    break;
                }
                case 3: {
                    value = buf.readFloatLE();
                    break;
                }
                default:
                    throw new IllegalArgumentException("Unknown game rule type " + grType);
            }
            rules.put(rule, value);
        }

        if (protocolVersion >= ProtocolConstants.MINECRAFT_PE_1_16_100) {
            int experimentCount = buf.readIntLE(); // Experiment count
            for (int i = 0; i< experimentCount; i++){
                DefinedPacket.readString(buf); // Experiment name
                buf.readBoolean(); // Enabled
            }
            buf.readBoolean(); // Were experiments previously toggled
        }

        buf.readBoolean(); // Bonus chest
        buf.readBoolean(); // Player map enabled
        if (protocolVersion < ProtocolConstants.MINECRAFT_PE_1_9) {
            buf.readBoolean(); // Trust players
        }
        DefinedPacket.readSVarInt(buf); // Permission level
        if (protocolVersion < ProtocolConstants.MINECRAFT_PE_1_9) {
            DefinedPacket.readSVarInt(buf); // Same publish setting
        }
        buf.readIntLE(); // Server chunk tick radius..
        if (protocolVersion < ProtocolConstants.MINECRAFT_PE_1_9) {
            buf.readBoolean(); // Platformbroadcast
            DefinedPacket.readSVarInt(buf); // Broadcast mode
            buf.readBoolean(); // Broadcast intent
        }
        buf.readBoolean(); // HasLockedRes pack
        buf.readBoolean(); // hasLockedBeh pack
        buf.readBoolean(); // HasLocked world template.
        buf.readBoolean(); // Microsoft GamerTags only. Hell no!
        buf.readBoolean(); // Is from world template
        buf.readBoolean(); // Is world template option locked
        if (protocolVersion >= ProtocolConstants.MINECRAFT_PE_1_12) {
            buf.readByte(); // Only spawn v1 villagers
        }
        if (protocolVersion >= ProtocolConstants.MINECRAFT_PE_1_13) {
            DefinedPacket.readString(buf); // Vanilla version
        }
        if (protocolVersion >= ProtocolConstants.MINECRAFT_PE_1_16){
            buf.readIntLE(); // WorldWidth
            buf.readIntLE(); // WorldDepth
            buf.readBoolean(); // NetherType
            buf.readBoolean(); // ExpGameplay
        }
        DefinedPacket.readString(buf); // Level ID (empty string)
        DefinedPacket.readString(buf); // World name (empty string)
        DefinedPacket.readString(buf); // Premium world template id (empty string)
        buf.readBoolean(); // Is trial
        if (protocolVersion >= ProtocolConstants.MINECRAFT_PE_1_13) {
            // Is server authoritative over movement
            if (protocolVersion >= ProtocolConstants.MINECRAFT_PE_1_16_100){
                DefinedPacket.readSVarInt(buf);
            }else {
                buf.readBoolean();
            }
        }
        buf.readLongLE(); // World ticks
        DefinedPacket.readSVarInt(buf); // Enchantment seed FFS MOJANG

        // Here starts the pain of block palette
        // Since protocol 419 servers does not send vanilla blocks
        // Servers may send custom added blocks only. But we don't support its rewrite yet.
        if (protocolVersion < ProtocolConstants.MINECRAFT_PE_1_16_100){
            this.parseBlockPalette(protocolVersion, payloadStartIndex, buf);
        }

        // Item palette here, but we don't care.
        // Skip the rest.
        buf.skipBytes(buf.readableBytes());
    }

    private void parseBlockPalette(int protocolVersion, int payloadStartIndex, ByteBuf buf){
        if (protocolVersion >= ProtocolConstants.MINECRAFT_PE_1_13) {
            final int paletteStart = buf.readerIndex();
            try {
                final ByteBufInputStream inputStream = new ByteBufInputStream(buf);
                NbtUtils.createNetworkReader(inputStream).readTag();
            } catch (IOException e) {
                throw new IllegalArgumentException("Failed to parse block palette!", e);
            }
            payloadPaletteOffset = paletteStart - payloadStartIndex;
            payloadPaletteLength = buf.readerIndex() - paletteStart;
        } else {
            final int paletteItems = DefinedPacket.readVarInt(buf);
            final int paletteStart = buf.readerIndex();
            for (int i = 0; i < paletteItems; i++) {
                final int strLength = DefinedPacket.readVarInt(buf);
                if (protocolVersion >= ProtocolConstants.MINECRAFT_PE_1_12) {
                    buf.skipBytes(strLength + 4);
                } else {
                    buf.skipBytes(strLength + 2);
                }
            }
            payloadPaletteOffset = paletteStart - payloadStartIndex;
            payloadPaletteLength = buf.readerIndex() - paletteStart;
        }
    }

    @Override
    public void write(ByteBuf buf, ProtocolConstants.Direction direction, int protocolVersion) {
        writeSVarLong(getEntityId(), buf);
        writeVarLong(getEntityId(), buf);
        writeSVarInt(getGameMode(), buf);
        buf.writeFloatLE(spawnX); // player x
        buf.writeFloatLE(spawnY); // player y
        buf.writeFloatLE(spawnZ); // player z
        buf.writeFloatLE(0); // player pitch
        buf.writeFloatLE(0); // player yaw
        writeSVarInt(0, buf); // seed
        if (protocolVersion >= ProtocolConstants.MINECRAFT_PE_1_16){
            buf.writeShort(0); // type
            DefinedPacket.writeString("", buf); // biome
        }
        writeSVarInt(PEDataValues.getPeDimensionId(getDimension()), buf); // world dimension
        writeSVarInt(1, buf); // world type (1 - infinite)
        writeSVarInt(0, buf); // world gamemode
        writeSVarInt(getDifficulty(), buf); // world difficulty
        buf.writeBytes(tailPayload);
    }
}
