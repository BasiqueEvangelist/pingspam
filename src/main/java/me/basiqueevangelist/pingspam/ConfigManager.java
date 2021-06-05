package me.basiqueevangelist.pingspam;

import blue.endless.jankson.Jankson;
import blue.endless.jankson.api.SyntaxError;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigManager<C> {
    private static final Jankson JANKSON = Jankson.builder().build();
    private static final Logger LOGGER = LogManager.getLogger("Pingspam/ConfigManager");

    private final Class<C> klass;
    private C config;
    private final String filename;

    public ConfigManager(Class<C> klass, C defaultInstance, String filename) {
        this.klass = klass;
        this.filename = filename;
        config = defaultInstance;
        load();
    }

    public C getConfig() {
        return config;
    }

    public void load() {
        Path confPath = FabricLoader.getInstance().getConfigDir().resolve(filename);
        if (Files.exists(confPath)) {
            try {
                config = JANKSON.fromJson(JANKSON.load(confPath.toFile()), klass);
            } catch (IOException | SyntaxError e) {
                LOGGER.error("Could not load config file!", e);
            }
        } else {
            save();
        }
    }

    public void save() {
        Path confPath = FabricLoader.getInstance().getConfigDir().resolve(filename);
        try {
            try (BufferedWriter bw = Files.newBufferedWriter(confPath)) {
                bw.write(JANKSON.toJson(config).toJson(true, true));
            }
        } catch (IOException e) {
            LOGGER.error("Could not load config file!", e);
        }
    }
}
