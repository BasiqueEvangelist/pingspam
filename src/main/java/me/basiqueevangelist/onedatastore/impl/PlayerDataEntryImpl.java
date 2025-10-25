package me.basiqueevangelist.onedatastore.impl;

import me.basiqueevangelist.onedatastore.api.Component;
import me.basiqueevangelist.onedatastore.api.ComponentInstance;
import me.basiqueevangelist.onedatastore.api.PlayerDataEntry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Uuids;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerDataEntryImpl implements PlayerDataEntry {
    private final UUID playerId;
    private final OneDataStoreState state;
    private final Map<Component<?, PlayerDataEntry>, ComponentInstance> components = new HashMap<>();

    public PlayerDataEntryImpl(OneDataStoreState state, UUID playerId) {
        this.playerId = playerId;
        this.state = state;

        for (Component<?, PlayerDataEntry> comp : OneDataStoreInit.PLAYER_COMPONENTS.values()) {
            components.put(comp, comp.factory().apply(this));
        }
    }

    public void fromTag(NbtCompound tag, RegistryWrapper.WrapperLookup registries) {
        for (Map.Entry<Component<?, PlayerDataEntry>, ComponentInstance> entry : components.entrySet()) {
            var tagName = entry.getKey().id().toString();

            if (tag.contains(tagName)) {
                try {
                    entry.getValue().fromTag(tag.getCompoundOrEmpty(tagName), registries);
                } catch (Exception e) {
                    OneDataStoreInit.LOGGER.error("Encountered error while deserializing {} for {}", tagName, playerId, e);
                }
            } else {
                entry.getValue().wasMissing();
            }
        }
    }

    public void wasMissing() {
        for (Map.Entry<Component<?, PlayerDataEntry>, ComponentInstance> entry : components.entrySet()) {
            entry.getValue().wasMissing();
        }
    }

    @Override
    public UUID playerId() {
        return playerId;
    }

    @Override
    public OneDataStoreState dataStore() {
        return state;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends ComponentInstance> T get(Component<T, PlayerDataEntry> component) {
        return (T) components.get(component);
    }

    public NbtCompound toTag(NbtCompound tag, RegistryWrapper.WrapperLookup registries) {
        tag.put("UUID", Uuids.CODEC, playerId);

        for (Map.Entry<Component<?, PlayerDataEntry>, ComponentInstance> entry : components.entrySet()) {
            var tagName = entry.getKey().id().toString();

            try {
                tag.put(tagName, entry.getValue().toTag(new NbtCompound(), registries));
            } catch (Exception e) {
                OneDataStoreInit.LOGGER.error("Encountered error while serializing {} for {}", tagName, playerId, e);
            }
        }

        return tag;
    }

    @Override
    public String toString() {
        return "PlayerDataEntryImpl{" +
            "playerId=" + playerId +
            '}';
    }
}
