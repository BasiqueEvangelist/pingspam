package me.basiqueevangelist.pingspam;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.datafixer.Schemas;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.WorldSavePath;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public enum OfflinePlayerCache {
    INSTANCE;

    private final Map<UUID, CompoundTag> savedPlayers = new HashMap<>();

    public void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(this::onServerStart);
        ServerLifecycleEvents.SERVER_STOPPED.register(this::onServerShutdown);
    }

    private void onServerStart(MinecraftServer server) {
        try {
            Path savedPlayersPath = server.getSavePath(WorldSavePath.PLAYERDATA);
            for (Path savedPlayerFile : Files.list(savedPlayersPath).collect(Collectors.toList())) {
                CompoundTag tag = NbtIo.readCompressed(savedPlayerFile.toFile());
                String filename = savedPlayerFile.getFileName().toString();
                String uuidStr = filename.substring(0, filename.lastIndexOf('.'));
                UUID uuid = UUID.fromString(uuidStr);
                int dataVersion = tag.contains("DataVersion", 3) ? tag.getInt("DataVersion") : -1;
                savedPlayers.put(uuid, NbtHelper.update(Schemas.getFixer(), DataFixTypes.PLAYER, tag, dataVersion));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void onServerShutdown(MinecraftServer server) {
        savedPlayers.clear();
    }

    public void onPlayerDataSaved(UUID forPlayer, CompoundTag tag) {
        savedPlayers.put(forPlayer, tag);
    }

    public Map<UUID, CompoundTag> getPlayers() {
        return savedPlayers;
    }
}
