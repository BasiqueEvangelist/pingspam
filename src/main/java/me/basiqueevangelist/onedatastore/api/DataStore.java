package me.basiqueevangelist.onedatastore.api;

import me.basiqueevangelist.onedatastore.impl.OneDataStoreState;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.ApiStatus;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

@ApiStatus.NonExtendable
public interface DataStore {
    static DataStore getFor(MinecraftServer server) {
        return OneDataStoreState.getFrom(server);
    }

    PlayerDataEntry getPlayerEntry(UUID playerId);

    <T extends ComponentInstance> T get(Component<T, DataStore> component);

    <T extends ComponentInstance> T getPlayer(UUID playerId, Component<T, PlayerDataEntry> component);

    Collection<PlayerDataEntry> players();
}
