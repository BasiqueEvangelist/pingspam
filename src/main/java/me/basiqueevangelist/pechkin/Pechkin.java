package me.basiqueevangelist.pechkin;

import me.basiqueevangelist.onedatastore.api.Component;
import me.basiqueevangelist.onedatastore.api.PlayerDataEntry;
import me.basiqueevangelist.pechkin.command.*;
import me.basiqueevangelist.pechkin.data.PechkinPlayerData;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;

import java.lang.ref.WeakReference;

public class Pechkin implements ModInitializer {
    public static final ConfigManager CONFIG = new ConfigManager();
    public static WeakReference<MinecraftServer> server;

    public static final Component<PechkinPlayerData, PlayerDataEntry> PLAYER_DATA = Component.registerPlayer(Identifier.of("pechkin", "player_data"), PechkinPlayerData::new);

    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTING.register(s -> {
            server = new WeakReference<>(s);
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            SendCommand.register(dispatcher);
            ListCommand.register(dispatcher);
            DeleteCommand.register(dispatcher);
            IgnoreCommand.register(dispatcher);
            ClearCommand.register(dispatcher);
            ReloadCommand.register(dispatcher);
        });
    }
}
