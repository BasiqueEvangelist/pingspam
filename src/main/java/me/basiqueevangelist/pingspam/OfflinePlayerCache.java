package me.basiqueevangelist.pingspam;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.datafixer.Schemas;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Util;
import net.minecraft.util.WorldSavePath;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public enum OfflinePlayerCache {
    INSTANCE;

    private final static Logger LOGGER = LogManager.getLogger("Pingspam/OfflinePlayerCache");
    private final Map<UUID, CompoundTag> savedPlayers = new HashMap<>();
    private MinecraftServer currentServer;

    public void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(this::onServerStart);
        ServerLifecycleEvents.SERVER_STOPPED.register(this::onServerShutdown);
    }

    private void onServerStart(MinecraftServer server) {
        currentServer = server;
        try {
            Path savedPlayersPath = server.getSavePath(WorldSavePath.PLAYERDATA);
            for (Path savedPlayerFile : Files.list(savedPlayersPath).collect(Collectors.toList())) {
                if (Files.isDirectory(savedPlayerFile) || !savedPlayerFile.toString().endsWith(".dat")) {
                    continue;
                }

                try {
                    CompoundTag tag = NbtIo.readCompressed(savedPlayerFile.toFile());
                    String filename = savedPlayerFile.getFileName().toString();
                    String uuidStr = filename.substring(0, filename.lastIndexOf('.'));
                    UUID uuid = UUID.fromString(uuidStr);
                    int dataVersion = tag.contains("DataVersion", 3) ? tag.getInt("DataVersion") : -1;
                    savedPlayers.put(uuid, NbtHelper.update(Schemas.getFixer(), DataFixTypes.PLAYER, tag, dataVersion));
                } catch (IOException e) {
                    LOGGER.error("Error while reading playerdata file {}: {}", savedPlayerFile, e);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void onServerShutdown(MinecraftServer server) {
        currentServer = null;
        savedPlayers.clear();
    }

    public void onPlayerDataSaved(UUID forPlayer, CompoundTag tag) {
        savedPlayers.put(forPlayer, tag);
    }

    public Map<UUID, CompoundTag> getPlayers() {
        return savedPlayers;
    }

    public CompoundTag get(UUID player) {
        return savedPlayers.get(player);
    }

    public CompoundTag reloadFor(UUID player) {
        try {
            Path savedPlayersPath = currentServer.getSavePath(WorldSavePath.PLAYERDATA);
            Path savedDataPath = savedPlayersPath.resolve(player.toString() + ".dat");
            CompoundTag tag = NbtIo.readCompressed(savedDataPath.toFile());
            savedPlayers.put(player, tag);
            return tag;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void saveFor(UUID player, CompoundTag newTag) {
        try {
            savedPlayers.put(player, newTag);
            File savedPlayersPath = currentServer.getSavePath(WorldSavePath.PLAYERDATA).toFile();
            File file = File.createTempFile(player.toString() + "-", ".dat", savedPlayersPath);
            NbtIo.writeCompressed(newTag, file);
            File newDataFile = new File(savedPlayersPath, player.toString() + ".dat");
            File oldDataFile = new File(savedPlayersPath, player.toString() + ".dat_old");
            Util.backupAndReplace(newDataFile, file, oldDataFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
