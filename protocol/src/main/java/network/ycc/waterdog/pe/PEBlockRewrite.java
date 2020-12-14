package network.ycc.waterdog.pe;

import io.netty.buffer.ByteBuf;

import net.md_5.bungee.protocol.DefinedPacket;
import net.md_5.bungee.protocol.ProtocolConstants;

public abstract class PEBlockRewrite {

    public final static PEBlockRewrite EMPTY = new PEBlockRewrite() {
        public int map(int id) {
            return id;
        }
    };

    protected static final int nV8Blocks = 16 * 16 * 16;

    public abstract int map(int id);

    public void rewriteClientbound(ByteBuf packet, int version) {
        final int readerIndex = packet.readerIndex();
        final int packetId = DefinedPacket.readVarInt(packet);
        final int writerIndex = packet.readerIndex();

        switch (packetId) { //same-size rewrites
            case 0x3a: //FULL_CHUNK_DATA_PACKET
                DefinedPacket.readSVarInt(packet); //chunk X
                DefinedPacket.readSVarInt(packet); //chunk Z
                final ByteBuf out = packet.alloc().ioBuffer(packet.readableBytes() + 128);
                int sections, rwStart;
                try {
                    if (version >= ProtocolConstants.MINECRAFT_PE_1_12) {
                        sections = packet.readShortLE();
                        rwStart = packet.readerIndex();
                        DefinedPacket.readVarInt(packet); //payload length
                    } else {
                        rwStart = packet.readerIndex();
                        DefinedPacket.readVarInt(packet); //payload length
                        sections = DefinedPacket.readVarInt(packet);
                        out.writeByte(sections);
                    }
                    rewriteChunkSections(packet, out, sections);

                    packet.readerIndex(readerIndex);
                    packet.writerIndex(rwStart);
                    DefinedPacket.writeVarInt(out.readableBytes(), packet); //payload length
                    packet.writeBytes(out);

                } finally {
                    out.release();
                }
                return;
            default: // NOOP
        }

        switch (packetId) { //varint rewrites
            case 0x15: { //UPDATE_BLOCK_PACKET
                final int x = DefinedPacket.readSVarInt(packet);
                final int y = DefinedPacket.readVarInt(packet);
                final int z = DefinedPacket.readSVarInt(packet);
                final int id = DefinedPacket.readVarInt(packet);
                final int flags = DefinedPacket.readVarInt(packet);
                final int layer = DefinedPacket.readVarInt(packet);

                packet.readerIndex(readerIndex);
                packet.writerIndex(writerIndex);

                DefinedPacket.writeSVarInt(x, packet);
                DefinedPacket.writeVarInt(y, packet);
                DefinedPacket.writeSVarInt(z, packet);
                DefinedPacket.writeVarInt(map(id), packet);
                DefinedPacket.writeVarInt(flags, packet);
                DefinedPacket.writeVarInt(layer, packet);
                return;
            }
            case 0x19: { //LEVEL_EVENT_PACKET
                final int evID = DefinedPacket.readSVarInt(packet);
                final float x = packet.readFloatLE();
                final float y = packet.readFloatLE();
                final float z = packet.readFloatLE();
                //should be a svarint, but nukkit does weird things sometimes
                int data = (int) DefinedPacket.readSVarLong(packet);

                switch (evID) {
                    case PEDataValues.LEVEL_EVENT_EVENT_TERRAIN_PARTICLE:
                    case PEDataValues.LEVEL_EVENT_EVENT_PARTICLE_PUNCH_BLOCK:
                    case PEDataValues.LEVEL_EVENT_EVENT_PARTICLE_DESTROY:
                        //only rewrite lower 16 bits
                        final int high = data & 0xFFFF0000;
                        final int blockID = map(data & 0xFFFF) & 0xFFFF;
                        data = high | blockID;
                        break;
                    default: // NOOP
                }

                packet.readerIndex(readerIndex);
                packet.writerIndex(writerIndex);

                DefinedPacket.writeSVarInt(evID, packet);
                packet.writeFloatLE(x);
                packet.writeFloatLE(y);
                packet.writeFloatLE(z);
                DefinedPacket.writeSVarInt(data, packet);
                return;
            }
            case 0x7b: { //PLAY_LEVEL_SOUND_EVENT_PACKET
                final int sound = DefinedPacket.readVarInt(packet);
                final float x = packet.readFloatLE();
                final float y = packet.readFloatLE();
                final float z = packet.readFloatLE();
                int data = DefinedPacket.readSVarInt(packet);
                final byte[] remaining = new byte[packet.readableBytes()];
                packet.readBytes(remaining);

                switch (sound) {
                    case 6: // SOUND_PLACE
                        data = map(data);
                        break;
                    default: // NOOP
                }

                packet.readerIndex(readerIndex);
                packet.writerIndex(writerIndex);

                DefinedPacket.writeVarInt(sound, packet);
                packet.writeFloatLE(x);
                packet.writeFloatLE(y);
                packet.writeFloatLE(z);
                DefinedPacket.writeSVarInt(data, packet);
                packet.writeBytes(remaining);
                return;
            }
            case 0x0D: //PLAY_ENTITY_SPAWN TODO: entity spawn for fall blocks, DATA_VARIANT
            default: // NOOP
        }

        packet.readerIndex(readerIndex);
    }

    protected void rewriteChunkSections(ByteBuf in, ByteBuf out, int sections) {
        for (int section = 0 ; section < sections ; section++) {
            boolean notSupported = false;
            final int subchunkVersion = in.readUnsignedByte();
            out.writeByte(subchunkVersion);
            switch (subchunkVersion) {
                case 0: //legacy block ids, no remap needed
                case 4: //minet uses this format. what is it?
                case 139:
                    out.writeBytes(in);
                    return;
                case 8: //new form chunk, baked-in palette
                    rewriteV8ChunkSection(in, out);
                    break;
                default: //unsupported
                    //TODO: erm, what changed here?
                    //throw new IllegalArgumentException("Unknown subchunk format " + subchunkVersion);
                    notSupported = true;
                    System.out.println("PEBlockRewrite: Unknown subchunk format " + subchunkVersion);
                    break;
            }
            if (notSupported) {
                break;
            }
        }
        out.writeBytes(in); //copy the rest
    }

    protected void rewriteV8ChunkSection(ByteBuf in, ByteBuf out) {
        final int storageCount = in.readUnsignedByte();
        out.writeByte(storageCount);

        for (int storage = 0 ; storage < storageCount ; storage++) {
            final int flags = in.readUnsignedByte();
            final int bitsPerBlock = flags >> 1; //isRuntime = (flags & 0x1) != 0
            final int blocksPerWord = Integer.SIZE / bitsPerBlock;
            final int nWords = (nV8Blocks + blocksPerWord - 1) / blocksPerWord;

            out.writeByte(flags);
            out.writeBytes(in, nWords * Integer.BYTES);

            final int nPaletteEntries = DefinedPacket.readSVarInt(in);
            DefinedPacket.writeSVarInt(nPaletteEntries, out);

            for (int i = 0 ; i < nPaletteEntries ; i++) {
                DefinedPacket.writeSVarInt(map(DefinedPacket.readSVarInt(in)), out);
            }
        }
    }

}
