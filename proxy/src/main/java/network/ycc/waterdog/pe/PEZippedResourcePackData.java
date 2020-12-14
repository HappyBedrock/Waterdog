package network.ycc.waterdog.pe;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.google.common.base.Preconditions;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import lombok.EqualsAndHashCode;

import lombok.Getter;
import lombok.ToString;
import net.md_5.bungee.protocol.DefinedPacket;
import net.md_5.bungee.protocol.ProtocolConstants;

@EqualsAndHashCode
@ToString
public class PEZippedResourcePackData implements PEResourcePackData {
    @Getter
    private final String name;
    @Getter
    private final UUID uuid;
    @Getter
    private final String version;
    @Getter
    private final byte[] sha256;
    private final ByteBuf data;

    public PEZippedResourcePackData(File file) throws IOException, IllegalArgumentException {
        if (!file.exists()) {
            throw new FileNotFoundException(file.toString());
        }

        final JsonObject manifest;
        try (final ZipFile zip = new ZipFile(file)) {
            final ZipEntry manifestEntry = zip.getEntry("manifest.json");
            if (manifestEntry == null) {
                throw new FileNotFoundException("manifest.json" + " in " + file.toString());
            }

            manifest = new JsonParser()
                    .parse(new InputStreamReader(zip.getInputStream(manifestEntry), StandardCharsets.UTF_8))
                    .getAsJsonObject();
        }
        checkForField(manifest, "format_version");
        checkForField(manifest, "header");
        checkForField(manifest, "modules");

        final JsonObject header = manifest.getAsJsonObject("header");
        checkForField(header, "description");
        checkForField(header, "name");
        checkForField(header, "uuid");
        checkForField(header, "version");

        final JsonArray versionArr = header.get("version").getAsJsonArray();

        final byte[] fileData = Files.readAllBytes(file.toPath());
        data = PooledByteBufAllocator.DEFAULT.ioBuffer(fileData.length).writeBytes(fileData);

        try {
            sha256 = MessageDigest.getInstance("SHA-256").digest(fileData);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("This JVM does not support SHA-256 digest", e);
        }

        name = header.get("name").getAsString();
        uuid = UUID.fromString(header.get("uuid").getAsString());
        version = versionArr.get(0).getAsString() + "." +
                  versionArr.get(1).getAsString() + "." +
                  versionArr.get(2).getAsString();
    }

    protected static void checkForField(JsonObject obj, String key1) {
        Preconditions.checkArgument(obj.has(key1), "Manifest missing " + key1);
    }

    public void writeChunk(int chunkIndex, ByteBuf to, int version) {
        final int offset = chunkIndex * CHUNK_SIZE;
        final int length = Math.min(getSize() - offset, CHUNK_SIZE);
        if (version >= ProtocolConstants.MINECRAFT_PE_1_13) {
            DefinedPacket.writeVarInt(length, to);
        } else {
            to.writeIntLE(length);
        }
        to.writeBytes(data, data.readerIndex() + offset, length);
    }

    public int getSize() {
        return data.readableBytes();
    }
}
