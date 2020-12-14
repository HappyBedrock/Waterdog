package network.ycc.waterdog.pe;

import com.google.common.base.Preconditions;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;

import net.md_5.bungee.protocol.DefinedPacket;
import net.md_5.bungee.protocol.ProtocolConstants;

import java.util.Map;
import java.util.UUID;

public class PERawPacketData {
    protected static final int PLAYER_ACTION_ID = 36;
    protected static final int EXT_PS_AWAIT_DIM_SWITCH_ACK_ID = -100;
    protected static final int DIMENSION_CHANGE_ACK = 14;

    private static final byte[] fakePEChunkData;
    private static final byte[] fakePEChunkData112;

    static {
        final ByteBuf serializer = Unpooled.buffer();
        final ByteBuf chunkdata = Unpooled.buffer();

        chunkdata.writeByte(1); //1 section
        chunkdata.writeByte(8); //New subchunk version!
        chunkdata.writeByte(1); //Zero blockstorages :O
        chunkdata.writeByte((1 << 1) | 1);  //Runtimeflag and palette id.
        chunkdata.writeZero(512);
        DefinedPacket.writeSVarInt(1, chunkdata); //Palette size
        DefinedPacket.writeSVarInt(0, chunkdata); //Air
        chunkdata.writeZero(512); //heightmap.
        chunkdata.writeZero(256); //Biomedata.
        chunkdata.writeByte(0); //borders

        chunkdata.markReaderIndex();
        DefinedPacket.writeVarInt(chunkdata.readableBytes(), serializer);
        serializer.writeBytes(chunkdata);
        fakePEChunkData = new byte[serializer.readableBytes()];
        serializer.readBytes(fakePEChunkData);

        // 1.12 chunks
        chunkdata.resetReaderIndex();
        ByteBuf buf112 = Unpooled.buffer();
        DefinedPacket.writeVarInt(chunkdata.readUnsignedByte(), buf112);
        buf112.writeByte(0);
        DefinedPacket.writeVarInt(chunkdata.readableBytes(), buf112);
        buf112.writeBytes(chunkdata);
        fakePEChunkData112 = new byte[buf112.readableBytes()];
        buf112.readBytes(fakePEChunkData112);

        Preconditions.checkArgument(fakePEChunkData.length > 0);
        Preconditions.checkArgument(fakePEChunkData112.length > 0);
    }

    public static int peekPacketId(ByteBuf from) {
        if (!from.isReadable()) {
            return -1;
        }
        try {
            return DefinedPacket.readVarInt(from.markReaderIndex());
        } finally {
            from.resetReaderIndex();
        }
    }

    public static boolean isDimSwitchAck(ByteBuf data) {
        if (peekPacketId(data) == PLAYER_ACTION_ID) {
            final ByteBuf copy = data.duplicate();
            DefinedPacket.readVarInt(copy);
            DefinedPacket.readVarLong(copy); // entity id
            return DefinedPacket.readSVarInt(copy) == DIMENSION_CHANGE_ACK;
        }
        return false;
    }

    public static void injectChunkPublisherUpdate(Channel channel, int x, int y, int z) {
        final ByteBuf publisherUpdate = channel.alloc().ioBuffer();
        DefinedPacket.writeVarInt(0x79, publisherUpdate); //CHUNK_PUBLISHER_UPDATE_PACKET
        DefinedPacket.writeSVarInt(x, publisherUpdate);
        DefinedPacket.writeVarInt(y, publisherUpdate);
        DefinedPacket.writeSVarInt(z, publisherUpdate);
        DefinedPacket.writeVarInt(300, publisherUpdate);
        channel.write(publisherUpdate);
    }

    public static void injectForcedDimChange(Channel channel, int version) {
        injectChunkPublisherUpdate(channel, 0, 0, 0);
        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 4; z++) {
                ByteBuf buffer = channel.alloc().ioBuffer();
                DefinedPacket.writeVarInt(0x3A, buffer); //PLAY_CHUNK_DATA
                DefinedPacket.writeSVarInt(x, buffer);
                DefinedPacket.writeSVarInt(z, buffer);
                if (version >= ProtocolConstants.MINECRAFT_PE_1_12) {
                    buffer.writeBytes(fakePEChunkData112);
                } else {
                    buffer.writeBytes(fakePEChunkData);
                }
                channel.write(buffer);
            }
        }
        final ByteBuf lockPacket = channel.alloc().ioBuffer();
        DefinedPacket.writeVarInt(EXT_PS_AWAIT_DIM_SWITCH_ACK_ID, lockPacket);
        channel.writeAndFlush(lockPacket);
    }

    public static void injectGameMode(Channel channel, int gamemode) {
        final ByteBuf gameMode = channel.alloc().ioBuffer();
        DefinedPacket.writeVarInt(0x3E, gameMode); //PLAY_PLAYER_GAME_TYPE
        DefinedPacket.writeSVarInt(gamemode, gameMode);
        channel.writeAndFlush(gameMode);
    }

    public static void injectRemoveEntity(Channel channel, long entityId) {
        final ByteBuf removeEntity = channel.alloc().ioBuffer();
        DefinedPacket.writeVarInt(0x0E, removeEntity); //PLAY_ENTITY_DESTROY
        DefinedPacket.writeSVarLong(entityId, removeEntity);
        channel.write(removeEntity);
    }

    public static void injectRemovePlayerList(Channel channel, UUID uuid) {
        final ByteBuf removePlayerList = channel.alloc().ioBuffer();
        DefinedPacket.writeVarInt(0x3F, removePlayerList); //PLAY_PLAYER_INFO
        DefinedPacket.writeVarInt(1, removePlayerList); //remove
        DefinedPacket.writeVarInt(1, removePlayerList); //1 item
        removePlayerList.writeLongLE(uuid.getMostSignificantBits());
        removePlayerList.writeLongLE(uuid.getLeastSignificantBits());
        channel.write(removePlayerList);
    }

    public static void injectResourcePackInfo(Channel channel, PEResourcePackData pack) {
        final ByteBuf resourcePackInfo = channel.alloc().ioBuffer();
        DefinedPacket.writeVarInt(0x52, resourcePackInfo); //PLAY_RESOURCE_PACK_INFO
        DefinedPacket.writeString(pack.getUuid().toString(), resourcePackInfo);
        resourcePackInfo.writeIntLE(PEResourcePackData.CHUNK_SIZE);
        resourcePackInfo.writeIntLE(pack.getNumberChunks());
        resourcePackInfo.writeLongLE(pack.getSize());
        DefinedPacket.writeVarInt(pack.getSha256().length, resourcePackInfo);
        resourcePackInfo.writeBytes(pack.getSha256());
        resourcePackInfo.writeBoolean(false); //premium
        resourcePackInfo.writeByte(1); //resource pack
        channel.write(resourcePackInfo);
    }

    public static void injectResourcePackData(Channel channel, PEResourcePackData pack, int chunkIndex, int version) {
        final ByteBuf resourcePackData = channel.alloc().ioBuffer();
        DefinedPacket.writeVarInt(0x53, resourcePackData); //PLAY_RESOURCE_PACK_DATA
        DefinedPacket.writeString(pack.getUuid().toString(), resourcePackData);
        resourcePackData.writeIntLE(chunkIndex);
        resourcePackData.writeLongLE(PEResourcePackData.CHUNK_SIZE * chunkIndex);
        pack.writeChunk(chunkIndex, resourcePackData, version);
        channel.write(resourcePackData);
    }

    public static void injectRemoveAllEffects(Channel channel, long clientEntityId) {
        for (int i = 0 ; i < 30 ; i++) {
            injectRemoveEntityEffect(channel, clientEntityId, i);
        }
        channel.flush();
    }

    public static void injectRemoveEntityEffect(Channel channel, long entityId, int effectId) {
        final ByteBuf removeEntityEffect = channel.alloc().ioBuffer();
        DefinedPacket.writeVarInt(0x1C, removeEntityEffect); //PLAY_ENTITY_EFFECT
        DefinedPacket.writeVarLong(entityId, removeEntityEffect);
        removeEntityEffect.writeByte(3); //remove effect
        DefinedPacket.writeSVarInt(effectId, removeEntityEffect);
        DefinedPacket.writeSVarInt(0, removeEntityEffect); // unused
        removeEntityEffect.writeBoolean(false); // unused
        DefinedPacket.writeSVarInt(0, removeEntityEffect); // unused
        channel.write(removeEntityEffect);
    }

    public static void injectSetGameRule(Channel channel, final Map<String, Object> rules){
        final ByteBuf gameRulePacket = channel.alloc().ioBuffer();
        DefinedPacket.writeVarInt(0x48, gameRulePacket); //GAME_RULES_CHANGED_PACKET
        DefinedPacket.writeVarInt(rules.size(), gameRulePacket);
        rules.forEach((String rule, Object value)->{
            DefinedPacket.writeString(rule, gameRulePacket);

            if (value instanceof Byte){
                DefinedPacket.writeVarInt(1, gameRulePacket);
                gameRulePacket.writeByte((byte) value);
            }else if (value instanceof Integer){
                DefinedPacket.writeVarInt(2, gameRulePacket);
                DefinedPacket.writeSVarInt((int) value, gameRulePacket);
            }else if (value instanceof Float){
                DefinedPacket.writeVarInt(3, gameRulePacket);
                gameRulePacket.writeFloatLE((float) value);
            }
        });
        channel.write(gameRulePacket);
    }
}
