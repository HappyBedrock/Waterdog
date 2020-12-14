package network.ycc.waterdog.pe;

import network.ycc.waterdog.pe.packet.PEResourcePack;
import network.ycc.waterdog.pe.packet.PEResourceStack;

import io.netty.channel.Channel;

import java.io.File;
import java.io.FileNotFoundException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import net.md_5.bungee.connection.InitialHandler;

public class PEResourcePackManager {
    public static final PEResourcePackManager INSTANCE = new PEResourcePackManager();

    private boolean loaded = false;
    private PEResourcePackData[] resourcePacks = new PEResourcePackData[0];
    private PEResourcePackData[] behaviorPacks = new PEResourcePackData[0];
    private Map<UUID, PEResourcePackData> packMap = new HashMap<>();

    private PEResourcePackManager() {}

    public synchronized void load(Logger logger) {
        if (loaded) {
            return;
        }
        loaded = true;

        final ArrayList<PEResourcePackData> rPacks = new ArrayList<>();
        final ArrayList<PEResourcePackData> bPacks = new ArrayList<>();
        File packsDir = new File("packs");

        if (!packsDir.exists()) {
            packsDir.mkdir();
        }

        for (File file : packsDir.listFiles()) {
            try {
                if (file.toString().toLowerCase().endsWith(".mcpack")) {
                    rPacks.add(new PEZippedResourcePackData(file));
                    logger.info("Loaded resource pack " + file.getName());
                } else if (file.toString().toLowerCase().endsWith(".mcaddon")) {
                    bPacks.add(new PEZippedResourcePackData(file));
                    logger.info("Loaded behavior pack " + file.getName());
                } else {
                    continue;
                }
            } catch (FileNotFoundException e) {
                logger.warning("File missing in pack " + file.getName() + ": " + e.getMessage());
            } catch (Exception e) {
                logger.warning("Failed to load pack " + file.getName() + ": " + e.getMessage());
            }
        }

        //TODO: check for duplicates?
        rPacks.forEach(pack -> packMap.put(pack.getUuid(), pack));
        bPacks.forEach(pack -> packMap.put(pack.getUuid(), pack));

        resourcePacks = rPacks.toArray(resourcePacks);
        behaviorPacks = bPacks.toArray(behaviorPacks);
    }

    public PEResourcePackData get(UUID uuid) {
        return packMap.get(uuid);
    }

    public boolean hasPacks() {
        return resourcePacks.length != 0 || behaviorPacks.length != 0;
    }

    //TODO: lets figure out an async pattern here eventually, shave off some RAM
    public void sendPackDataChunk(Channel channel, UUID uuid, int chunkIndex, int version) {
        PERawPacketData.injectResourcePackData(channel, packMap.get(uuid), chunkIndex, version);
    }

    public void sendResourcePack(InitialHandler handler) {
        handler.unsafe().sendPacket(new PEResourcePack(hasPacks(), resourcePacks, behaviorPacks));
    }

    public void sendResourceStack(InitialHandler handler) {
        handler.unsafe().sendPacket(new PEResourceStack(hasPacks(), true, resourcePacks, behaviorPacks, "1.16.04"));
    }
}
