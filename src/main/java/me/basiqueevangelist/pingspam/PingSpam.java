package me.basiqueevangelist.pingspam;

import me.basiqueevangelist.onedatastore.api.Component;
import me.basiqueevangelist.onedatastore.api.DataStore;
import me.basiqueevangelist.onedatastore.api.PlayerDataEntry;
import me.basiqueevangelist.pingspam.commands.PingSpamCommands;
import me.basiqueevangelist.pingspam.data.PingspamGlobalData;
import me.basiqueevangelist.pingspam.data.PingspamPlayerData;
import me.basiqueevangelist.pingspam.network.PingSpamPackets;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.ApiStatus;
import org.slf4j.LoggerFactory;

public class PingSpam implements ModInitializer {
    public static final ConfigManager CONFIG = new ConfigManager();
    @ApiStatus.Internal
    public static MinecraftServer SERVER;

    public static final Component<PingspamPlayerData, PlayerDataEntry> PLAYER_DATA = Component.registerPlayer(new Identifier("pingspam", "player_data"), unused -> new PingspamPlayerData());
    public static final Component<PingspamGlobalData, DataStore> GLOBAL_DATA = Component.registerGlobal(new Identifier("pingspam", "global_data"), PingspamGlobalData::new);

    @Override
    public void onInitialize() {
        LoggerFactory.getLogger("PingSpam").info("Several people are typing...");

        PingSpamPackets.register();
        ServerLifecycleEvents.START_DATA_PACK_RELOAD.register((server, resourceManager) -> CONFIG.load());
        CommandRegistrationCallback.EVENT.register(PingSpamCommands::register);

        ServerLifecycleEvents.SERVER_STARTED.register(server -> SERVER = server);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> SERVER = null);
    }
}
