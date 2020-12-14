package network.ycc.waterdog.pe;

import com.google.common.collect.MapMaker;

import com.nukkitx.nbt.stream.NBTInputStream;
import com.nukkitx.nbt.stream.NetworkDataInputStream;
import com.nukkitx.nbt.tag.CompoundTag;
import com.nukkitx.nbt.tag.ListTag;
import io.netty.buffer.ByteBuf;

import io.netty.buffer.ByteBufInputStream;
import it.unimi.dsi.fastutil.objects.Object2ShortLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ShortMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectMap;

import net.md_5.bungee.protocol.DefinedPacket;
import net.md_5.bungee.protocol.ProtocolConstants;

import org.apache.commons.lang3.tuple.Pair;

import java.io.DataInput;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;

public final class PEBlockPalette {

    static ConcurrentMap<UUID, PEBlockPalette> paletteCache =
            new MapMaker().weakValues().concurrencyLevel(8).makeMap();

    public static PEBlockPalette get(ByteBuf paletteData, int version) {
        final UUID uuid = nameUUIDFromBytes(paletteData);
        PEBlockPalette cached = paletteCache.get(uuid);
        if (cached == null) {
            cached = new PEBlockPalette(paletteData, version, uuid);
            // lazy thread safe. idempotent, but might be set multiple times
            paletteCache.put(uuid, cached);
        }
        return cached;
    }

    static UUID nameUUIDFromBytes(ByteBuf buf) {
        final MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException nsae) {
            throw new InternalError("MD5 not supported", nsae);
        }

        buf.markReaderIndex();
        while (buf.isReadable()) {
            md.update(buf.readByte());
        }
        buf.resetReaderIndex();

        final byte[] md5Bytes = md.digest();
        md5Bytes[6]  &= 0x0f;  /* clear version        */
        md5Bytes[6]  |= 0x30;  /* set to version 3     */
        md5Bytes[8]  &= 0x3f;  /* clear variant        */
        md5Bytes[8]  |= 0x80;  /* set to IETF variant  */

        long msb = 0;
        long lsb = 0;
        assert md5Bytes.length == 16 : "data must be 16 bytes in length";
        for (int i = 0 ; i < 8 ; i++) {
            msb = (msb << 8) | (md5Bytes[i] & 0xff);
        }
        for (int i = 8 ; i < 16 ; i++) {
            lsb = (lsb << 8) | (md5Bytes[i] & 0xff);
        }

        return new UUID(msb, lsb);
    }

    final UUID uuid;

    Object2ShortMap<BlockPair> entryToId = new Object2ShortLinkedOpenHashMap<>();
    Short2ObjectMap<BlockPair> idToEntry = new Short2ObjectLinkedOpenHashMap<>();

    public PEBlockRewrite createRewrite(PEBlockPalette to) {
        if (PEBlockPalette.this == to || uuid.equals(to.uuid)) {
            return PEBlockRewrite.EMPTY;
        }
        return new PEBlockRewrite() {
            public int map(int id) {
                return to.getId(getEntry(id));
            }
        };
    }

    PEBlockPalette(ByteBuf buf, int version, UUID uuid) {
        this.uuid = uuid;

        if (version >= ProtocolConstants.MINECRAFT_PE_1_13) {
            int id = 0;
            try {
                final DataInput is = new NetworkDataInputStream(new ByteBufInputStream(buf));
                ListTag<CompoundTag> tag = (ListTag<CompoundTag>) new NBTInputStream(is).readTag();

                for (CompoundTag item : tag.getValue()) {
                    final CompoundTag block = item.getAsCompound("block");
                    addEntry((short) id++,
                            block.getAsString("name"),
                            block.getAsCompound("states")
                    );
                }
            } catch (IOException e) {
                throw new IllegalArgumentException("Failed to parse palette NBT", e);
            }
        } else {
            int id = 0;
            while (buf.isReadable()) {
                final String name = DefinedPacket.readString(buf);
                final short data = buf.readShortLE();
                if (version >= ProtocolConstants.MINECRAFT_PE_1_12) {
                    buf.readShortLE(); //what does this id do?
                }

                addEntry((short) id++, name, data);
            }
        }
    }

    void addEntry(short id, String name, Object data) {
        final BlockPair pair = new BlockPair(name, data);
        entryToId.put(pair, id);
        idToEntry.put(id, pair);
    }

    int getId(BlockPair entry) {
        return entryToId.getShort(entry) & 0xFFFF;
    }

    public UUID getUUID() {
        return uuid;
    }

    BlockPair getEntry(int id) {
        return idToEntry.get((short) id);
    }

    public static final class BlockPair extends Pair<String, Object> {
        final String key;
        final Object data;
        final int hash;

        public BlockPair(String name, Object data) {
            this.key = name;
            this.data = data;
            hash = super.hashCode();
        }

        public String getLeft() {
            return key;
        }

        public Object getRight() {
            return data;
        }

        public Object setValue(Object value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }

}

