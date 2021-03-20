package me.basiqueevangelist.pingspam;

import me.basiqueevangelist.pingspam.commands.PingSpamCommands;
import me.basiqueevangelist.pingspam.network.PingSpamPackets;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import org.apache.logging.log4j.LogManager;

public class PingSpam implements ModInitializer {
    public static final ConfigManager CONFIG = new ConfigManager();
    public static MinecraftServer SERVER;

    @Override
    public void onInitialize() {
        LogManager.getLogger("PingSpam").info("Several people are typing...");

        OfflinePlayerCache.INSTANCE.register();
        PingSpamPackets.register();
        ServerLifecycleEvents.START_DATA_PACK_RELOAD.register((server, resourceManager) -> CONFIG.load());
        CommandRegistrationCallback.EVENT.register(PingSpamCommands::register);
        ServerLifecycleEvents.SERVER_STARTED.register((server -> SERVER = server));
    }
}
