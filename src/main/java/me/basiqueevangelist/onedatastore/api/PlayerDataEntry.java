package me.basiqueevangelist.onedatastore.api;

import org.jetbrains.annotations.ApiStatus;

import java.util.UUID;

@ApiStatus.NonExtendable
public interface PlayerDataEntry {
    UUID playerId();

    DataStore dataStore();

    <T extends ComponentInstance> T get(Component<T, PlayerDataEntry> component);
}
