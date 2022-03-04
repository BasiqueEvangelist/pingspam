package me.basiqueevangelist.pingspam.utils;

import me.basiqueevangelist.pingspam.PingSpam;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.datafixer.Schemas;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtIo;
import net.minecraft.util.WorldSavePath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

public final class OfflineUtil {
    private final static Logger LOGGER = LoggerFactory.getLogger("Pingspam/OfflineUtil");

    private OfflineUtil() {

    }

    // Directly copied from NeVSeti/OwO Offline API

    public static NbtCompound get(UUID player) throws IOException {
        Path savedPlayersPath = PingSpam.SERVER.getSavePath(WorldSavePath.PLAYERDATA);
        Path savedDataPath = savedPlayersPath.resolve(player.toString() + ".dat");
        NbtCompound rawTag = NbtIo.readCompressed(savedDataPath.toFile());
        int dataVersion = rawTag.contains("DataVersion", 3) ? rawTag.getInt("DataVersion") : -1;
        return NbtHelper.update(Schemas.getFixer(), DataFixTypes.PLAYER, rawTag, dataVersion);
    }

    public static List<UUID> listSavedPlayers() {
        List<UUID> list = new ArrayList<>();
        Path savedPlayersPath = PingSpam.SERVER.getSavePath(WorldSavePath.PLAYERDATA);

        try (Stream<Path> stream = Files.list(savedPlayersPath)) {
            for (Path savedPlayerFile : (Iterable<? extends Path>) stream::iterator) {
                if (Files.isDirectory(savedPlayerFile) || !savedPlayerFile.toString().endsWith(".dat")) {
                    continue;
                }

                try {
                    String filename = savedPlayerFile.getFileName().toString();
                    String uuidStr = filename.substring(0, filename.lastIndexOf('.'));
                    UUID uuid = UUID.fromString(uuidStr);
                    list.add(uuid);
                } catch (IllegalArgumentException iae) {
                    LOGGER.error("Encountered invalid UUID in playerdata directory! ", iae);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return list;
    }
}
