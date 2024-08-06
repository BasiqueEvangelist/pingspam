package me.basiqueevangelist.onedatastore.impl;

import me.basiqueevangelist.onedatastore.api.Component;
import me.basiqueevangelist.onedatastore.api.ComponentInstance;
import me.basiqueevangelist.onedatastore.api.DataStore;
import me.basiqueevangelist.onedatastore.api.PlayerDataEntry;
import me.basiqueevangelist.onedatastore.impl.command.PurgeCommand;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class OneDataStoreInit implements ModInitializer {
    static final Logger LOGGER = LoggerFactory.getLogger("OneDataStore");

    public static final Map<Identifier, Component<?, PlayerDataEntry>> PLAYER_COMPONENTS = new HashMap<>();
    public static final Map<Identifier, Component<?, DataStore>> GLOBAL_COMPONENTS = new HashMap<>();

    @Override
    public void onInitialize() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            OneDataStoreState.getFrom(server).getPlayerEntry(handler.player.getUuid());
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            PurgeCommand.register(dispatcher);
        });
    }

    public static <T extends ComponentInstance> Component<T, PlayerDataEntry> registerPlayerComponent(Identifier id, Function<PlayerDataEntry, T> factory) {
        ComponentImpl<T, PlayerDataEntry> component = new ComponentImpl<>(id, factory);
        PLAYER_COMPONENTS.put(id, component);
        return component;
    }

    public static <T extends ComponentInstance> Component<T, DataStore> registerGlobalComponent(Identifier id, Function<DataStore, T> factory) {
        ComponentImpl<T, DataStore> component = new ComponentImpl<>(id, factory);
        GLOBAL_COMPONENTS.put(id, component);
        return component;
    }
}
