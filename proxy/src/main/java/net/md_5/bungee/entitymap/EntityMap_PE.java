package net.md_5.bungee.entitymap;

import io.netty.buffer.ByteBuf;

import net.md_5.bungee.protocol.DefinedPacket;
import net.md_5.bungee.protocol.ProtocolConstants;

import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.LongConsumer;

public class EntityMap_PE extends EntityMap {

    final int version;

    public EntityMap_PE(int version) {
        this.version = version;
    }

    @Override
    public void rewriteServerbound(ByteBuf packet, int oldId, int newId) {
        doRewrite(packet, oldId, newId);
    }

    @Override
    public void rewriteClientbound(ByteBuf from, int oldId, int newId) {
        doRewrite(from, oldId, newId);
    }

    public void doRewrite(ByteBuf from, int oldId, int newId) {
        if (!from.isReadable()) {
            return;
        }
        final ByteBuf to = from.duplicate();
        final int originalReader = from.readerIndex();
        final int packetId = DefinedPacket.readVarInt(from);
        final Consumer<Rewrite> act = cmd -> cmd.rewrite(from, to, oldId, newId);

        to.readerIndex(to.readerIndex() - 16); //rewrite in place, but with 16 byte lead
        to.writerIndex(to.readerIndex());
        DefinedPacket.writeVarInt(packetId, to);

        switch (packetId) {
            case 0x12: //PLAY_ENTITY_TELEPORT
            case 0x1B: //PLAY_ENTITY_STATUS
            case 0x1C: //PLAY_ENTITY_EFFECT
            case 0x1D: //PLAY_ENTITY_ATTRIBUTES
            case 0x1F: //PLAY_MOB_EQUIPMENT
            case 0x24: //PLAY_PLAYER_ACTION
            case 0x27: //PLAY_ENTITY_METADATA
            case 0x28: //PLAY_ENTITY_VELOCITY
            case 0x6F: //PLAY_MOVE_ENTITY_DELTA
            case 0x71: //PLAY_LOCAL_PLAYER_INITIALIZED
            case 0x8a: //EMOTE_PACKET
                act.accept(VARLONG);
                break;
            case 0x0C: //PLAY_SPAWN_PLAYER
                to.writeBytes(from, Long.BYTES * 2);
                act.accept(SK_VI_DATA);
                act.accept(SVARLONG);
                act.accept(VARLONG);
                break;
            case 0x0D: //PLAY_ENTITY_SPAWN
            case 0x0F: //PLAY_ADD_ITEM_ENTITY
            case 0x16: //PLAY_ADD_PAINTING_PACKET
                act.accept(SVARLONG); //unique id
                act.accept(VARLONG); //runtime id
                break;
            case 0x0E: //PLAY_ENTITY_DESTROY
            case 0x4A: //PLAY_BOSS_EVENT
                act.accept(SVARLONG);
                break;
            case 0x11: //PLAY_ENTITY_COLLECT_EFFECT
                act.accept(VARLONG);
                act.accept(VARLONG);
                break;
            case 0x13: //PLAY_PLAYER_MOVE_LOOK
                act.accept(VARLONG);
                to.writeBytes(from, Float.BYTES * 6 + Byte.BYTES * 2);
                act.accept(VARLONG);
                break;
            case 0x21: //PLAY_INTERACT
                to.writeBytes(from, 1);
                act.accept(VARLONG);
                break;
            case 0x29: //PLAY_ENTITY_PASSENGER
                act.accept(SVARLONG);
                act.accept(SVARLONG);
                break;
            case 0x2C: //PLAY_ENTITY_ANIMATION
                act.accept(SK_SVARINT);
                act.accept(VARLONG);
                break;
            //TODO: case 0x2D: //RESPAWN
            case 0x37: { //PLAY_ADVENTURE_SETTINGS
                final int idIndex = from.writerIndex() - Long.BYTES;
                final long id = from.getLongLE(idIndex);
                from.setLongLE(idIndex, id == oldId ? newId : (id == newId ? oldId : id));
                from.readerIndex(originalReader);
                return;
            }
            case 0x3F: //PLAY_PLAYER_INFO
                final int type = from.readUnsignedByte();
                final int num = DefinedPacket.readVarInt(from);
                to.writeByte(type);
                DefinedPacket.writeVarInt(num, to);
                if (type == 0) { //ADD
                    for (int i = 0; i < num; i++) {
                        to.writeBytes(from, Long.BYTES * 2); //UUID

                        //DefinedPacket.writeVarLong(DefinedPacket.readVarLong(from), to); - will not rewrite entity ID
                        act.accept(VARLONG); //entity ID

                        act.accept(SK_VI_DATA); //username
                        if (version < ProtocolConstants.MINECRAFT_PE_1_13) {
                            act.accept(SK_VI_DATA); //skin name
                            act.accept(SK_VI_DATA); //skin data
                            act.accept(SK_VI_DATA); //cape data
                            act.accept(SK_VI_DATA); //geom name
                            act.accept(SK_VI_DATA); //geom data
                        }
                        act.accept(SK_VI_DATA); //xuid
                        act.accept(SK_VI_DATA); //channel
                        if (version >= ProtocolConstants.MINECRAFT_PE_1_13) {
                            to.writeBytes(from, Integer.BYTES); //build platform
                            //serialized skin data
                            act.accept(SK_VI_DATA); //skin id
                            act.accept(SK_VI_DATA); //skin resource patch
                            to.writeBytes(from, Integer.BYTES); //width
                            to.writeBytes(from, Integer.BYTES); //height
                            act.accept(SK_VI_DATA); //skin data
                            final int animatedImageData = from.readIntLE();
                            to.writeIntLE(animatedImageData);
                            for (int j = 0; j < animatedImageData; j++) {
                                to.writeBytes(from, Integer.BYTES); //image width
                                to.writeBytes(from, Integer.BYTES); //image height
                                act.accept(SK_VI_DATA); //image
                                to.writeBytes(from, Integer.BYTES); //type
                                to.writeBytes(from, Float.BYTES); //frames
                                if (version >= ProtocolConstants.MINECRAFT_PE_1_16_100){
                                    to.writeBytes(from, Integer.BYTES); // AnimationExpressionType
                                }
                            }
                            to.writeBytes(from, Integer.BYTES); //cape width
                            to.writeBytes(from, Integer.BYTES); //cape height
                            act.accept(SK_VI_DATA); //cape data
                            act.accept(SK_VI_DATA); //skin geom data
                            act.accept(SK_VI_DATA); //serialized animation data
                            to.writeBytes(from, Byte.BYTES); // is premium skin
                            to.writeBytes(from, Byte.BYTES); // is persona skin
                            to.writeBytes(from, Byte.BYTES); // is persona cape on classic skin
                            act.accept(SK_VI_DATA); //cape id
                            act.accept(SK_VI_DATA); //full skin id

                            if (version >= ProtocolConstants.MINECRAFT_PE_1_14_HOTFIX) {
                                act.accept(SK_VI_DATA); //arm length
                                act.accept(SK_VI_DATA); //skin color
                                final int personaPieces = from.readIntLE(); //persona piece count
                                to.writeIntLE(personaPieces);

                                for (int i1 = 0; i1 < personaPieces; i1++) {
                                    act.accept(SK_VI_DATA); //pieceId
                                    act.accept(SK_VI_DATA); //pieceType
                                    act.accept(SK_VI_DATA); //packId
                                    to.writeBoolean(from.readBoolean()); //isDefaultPiece
                                    act.accept(SK_VI_DATA); //productId
                                }

                                final int pieceTintColorCount = from.readIntLE(); //Piece tint color count
                                to.writeIntLE(pieceTintColorCount);

                                for (int i2 = 0; i2 < pieceTintColorCount; i2++) {
                                    act.accept(SK_VI_DATA); //pieceType

                                    final int colorCount = from.readIntLE(); //colorCount
                                    to.writeIntLE(colorCount);

                                    for (int i3 = 0; i3 < colorCount; i3++) {
                                        act.accept(SK_VI_DATA); //another color
                                    }
                                }
                            }
                            to.writeBytes(from, Byte.BYTES); //is teacher
                            to.writeBytes(from, Byte.BYTES); //is host
                        }
                    }
                    if (version >= ProtocolConstants.MINECRAFT_PE_1_14_HOTFIX) {
                        for (int i4 = 0; i4 < num; i4++) {
                            if(from.isReadable()){
                                to.writeBytes(from, Byte.BYTES); //isTrusted
                            }
                        }
                    }
                }
                break;
            case 0x50: //PLAY_TRADE_UPDATE
                to.writeBytes(from, 2);
                act.accept(SK_SVARINT);
                act.accept(SK_SVARINT);
                act.accept(SK_SVARINT);
                to.writeBytes(from, 1);
                act.accept(SVARLONG);
                act.accept(SVARLONG);
                break;
            default:
                from.readerIndex(originalReader);
                return;
        }

        to.writeBytes(from); //copy the rest

        from.readerIndex(to.readerIndex());
        from.writerIndex(to.writerIndex());
    }

    public void entityTrack(ByteBuf packet, LongConsumer add, LongConsumer remove) {
        if (!packet.isReadable()) {
            return;
        }
        final int readerIndex = packet.readerIndex();
        switch (DefinedPacket.readVarInt(packet)) {
            case 0x0C: //PLAY_SPAWN_PLAYER
                packet.skipBytes(Long.BYTES * 2);
                DefinedPacket.readString(packet);
                DefinedPacket.readSVarLong(packet); //unique ID
                add.accept(DefinedPacket.readVarLong(packet));
                break;
            case 0x0D: //PLAY_ENTITY_SPAWN
            case 0x0F: //PLAY_ADD_ITEM_ENTITY
            case 0x16: //PLAY_ADD_PAINTING_PACKET
                DefinedPacket.readSVarLong(packet);
                add.accept(DefinedPacket.readVarLong(packet));
                break;
            case 0x0E: //PLAY_ENTITY_DESTROY
                remove.accept(DefinedPacket.readSVarLong(packet));
                break;
            default: // NOOP
        }

        packet.readerIndex(readerIndex);
    }

    public void playerListTrackRewrite(ByteBuf packet, Consumer<UUID> add, Consumer<UUID> remove, UUID playerUUID, UUID loginUUID) {
        if (!packet.isReadable()) {
            return;
        }
        final int readerIndex = packet.readerIndex();
        final Runnable rewrite = () -> {
            final int preUUIDReaderIndex = packet.readerIndex();
            final long msb = packet.readLongLE();
            final long lsb = packet.readLongLE();
            if (msb == playerUUID.getMostSignificantBits()
                    && lsb == playerUUID.getLeastSignificantBits()) {
                packet.setLongLE(preUUIDReaderIndex, loginUUID.getMostSignificantBits());
                packet.setLongLE(preUUIDReaderIndex + Long.BYTES, loginUUID.getLeastSignificantBits());
            } else if (msb == loginUUID.getMostSignificantBits()
                    && lsb == loginUUID.getLeastSignificantBits()) {
                packet.setLongLE(preUUIDReaderIndex, playerUUID.getMostSignificantBits());
                packet.setLongLE(preUUIDReaderIndex + Long.BYTES, playerUUID.getLeastSignificantBits());
            }
        };
        switch (DefinedPacket.readVarInt(packet)) {
            case 0x3F: //PLAY_PLAYER_INFO
                final int type = packet.readUnsignedByte();
                final int num = DefinedPacket.readVarInt(packet);
                if (type == 0) { //ADD
                    for (int i = 0; i < num; i++) {
                        final int preUUIDReaderIndex = packet.readerIndex();
                        add.accept(new UUID(packet.readLongLE(), packet.readLongLE()));
                        packet.readerIndex(preUUIDReaderIndex);

                        rewrite.run();
                        DefinedPacket.readVarLong(packet); //entity id
                        packet.skipBytes(DefinedPacket.readVarInt(packet)); //username
                        if (version < ProtocolConstants.MINECRAFT_PE_1_13) {
                            packet.skipBytes(DefinedPacket.readVarInt(packet)); //skin name
                            packet.skipBytes(DefinedPacket.readVarInt(packet)); //skin data
                            packet.skipBytes(DefinedPacket.readVarInt(packet)); //cape data
                            packet.skipBytes(DefinedPacket.readVarInt(packet)); //geom name
                            packet.skipBytes(DefinedPacket.readVarInt(packet)); //geom data
                        }
                        packet.skipBytes(DefinedPacket.readVarInt(packet)); //xuid
                        packet.skipBytes(DefinedPacket.readVarInt(packet)); //channel
                        if (version >= ProtocolConstants.MINECRAFT_PE_1_13) {
                            packet.skipBytes(Integer.BYTES); //build platform
                            //serialized skin data
                            packet.skipBytes(DefinedPacket.readVarInt(packet)); //skin id
                            packet.skipBytes(DefinedPacket.readVarInt(packet)); //skin resource patch
                            packet.skipBytes(Integer.BYTES); //width
                            packet.skipBytes(Integer.BYTES); //height
                            packet.skipBytes(DefinedPacket.readVarInt(packet)); //skin data

                            final int animatedImageData = packet.readIntLE();
                            for (int j = 0; j < animatedImageData; j++) {
                                packet.skipBytes(Integer.BYTES); //image width
                                packet.skipBytes(Integer.BYTES); //image height
                                packet.skipBytes(DefinedPacket.readVarInt(packet)); //image
                                packet.skipBytes(Integer.BYTES); //type
                                packet.skipBytes(Float.BYTES); //frames
                                if (version >= ProtocolConstants.MINECRAFT_PE_1_16_100){
                                    packet.skipBytes(Integer.BYTES); // AnimationExpressionType
                                }
                            }

                            packet.skipBytes(Integer.BYTES); //cape width
                            packet.skipBytes(Integer.BYTES); //cape height
                            packet.skipBytes(DefinedPacket.readVarInt(packet)); //cape data
                            packet.skipBytes(DefinedPacket.readVarInt(packet)); //skin geom data
                            packet.skipBytes(DefinedPacket.readVarInt(packet)); //serialized animation data
                            packet.skipBytes(Byte.BYTES); // is premium skin
                            packet.skipBytes(Byte.BYTES); // is persona skin
                            packet.skipBytes(Byte.BYTES); // is persona cape on classic skin
                            packet.skipBytes(DefinedPacket.readVarInt(packet)); //cape id
                            packet.skipBytes(DefinedPacket.readVarInt(packet)); //full skin id

                            if (version >= ProtocolConstants.MINECRAFT_PE_1_14_HOTFIX) {
                                packet.skipBytes(DefinedPacket.readVarInt(packet)); // armSize
                                packet.skipBytes(DefinedPacket.readVarInt(packet)); // skinColor
                                final int personaPieceCount = packet.readIntLE(); // personaPieceCount
                                for (int i1 = 0; i1 < personaPieceCount; i1++) {
                                    packet.skipBytes(DefinedPacket.readVarInt(packet)); // pieceId
                                    packet.skipBytes(DefinedPacket.readVarInt(packet)); // pieceType
                                    packet.skipBytes(DefinedPacket.readVarInt(packet)); // packId
                                    packet.skipBytes(Byte.BYTES);
                                    packet.skipBytes(DefinedPacket.readVarInt(packet)); // productId
                                }
                                final int pieceTintColorCount = packet.readIntLE(); // Piece Tint Color
                                for (int i2 = 0; i2 < pieceTintColorCount; i2++) {
                                    packet.skipBytes(DefinedPacket.readVarInt(packet)); // Piece Type
                                    final int colorAmount = packet.readIntLE(); // Color Amount
                                    for (int i3 = 0; i3 < colorAmount; i3++) {
                                        packet.skipBytes(DefinedPacket.readVarInt(packet)); // Another Color
                                    }
                                }
                            }
                            packet.skipBytes(Byte.BYTES); // is teacher
                            packet.skipBytes(Byte.BYTES); // is host
                        }
                    }
                    for (int i1 = 0; i1 < num; i1++){
                        if(packet.isReadable()) {
                            packet.skipBytes(Byte.BYTES);
                        }
                    }

                } else if (type == 1) { //REMOVE
                    for (int i = 0; i < num; i++) {
                        final long msb = packet.readLongLE();
                        final long lsb = packet.readLongLE();
                        remove.accept(new UUID(msb, lsb));
                    }
                }
                break;
            case 0x5D: { //PLAY_PLAYER_SKIN_PACKET
                rewrite.run();
                break;
            }
            default: // NOOP
        }

        packet.readerIndex(readerIndex);
    }

    public interface Rewrite {
        void rewrite(ByteBuf from, ByteBuf to, int oldId, int newId);
    }

    // THIS IS JUST A FANCY WORD FOR A STRING YOU DUMBSHIT
    static public final Rewrite SK_VI_DATA = (from, to, oldId, newId) -> {
        final int length = DefinedPacket.readVarInt(from);
        DefinedPacket.writeVarInt(length, to);
        to.writeBytes(from, length);
    };

    static public final Rewrite SK_SVARINT = (from, to, oldId, newId) ->
            DefinedPacket.writeSVarInt(DefinedPacket.readSVarInt(from), to);

    static public final Rewrite SK_VARINT = (from, to, oldId, newId) ->
            DefinedPacket.writeVarInt(DefinedPacket.readVarInt(from), to);

    static public final Rewrite VARLONG = (from, to, oldId, newId) -> {
        final long id = DefinedPacket.readVarLong(from);
        DefinedPacket.writeVarLong(id == oldId ? newId : (id == newId ? oldId : id), to);
    };

    static public final Rewrite SVARLONG = (from, to, oldId, newId) -> {
        final long id = DefinedPacket.readSVarLong(from);
        DefinedPacket.writeSVarLong(id == oldId ? newId : (id == newId ? oldId : id), to);
    };

}