package me.basiqueevangelist.onedatastore.impl;

import me.basiqueevangelist.onedatastore.api.Component;
import me.basiqueevangelist.onedatastore.api.ComponentInstance;
import me.basiqueevangelist.onedatastore.api.PlayerDataEntry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;

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

    public void fromTag(NbtCompound tag) {
        for (Map.Entry<Component<?, PlayerDataEntry>, ComponentInstance> entry : components.entrySet()) {
            var tagName = entry.getKey().id().toString();

            if (tag.contains(tagName, NbtElement.COMPOUND_TYPE)) {
                try {
                    entry.getValue().fromTag(tag.getCompound(tagName));
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

    public NbtCompound toTag(NbtCompound tag) {
        tag.put("UUID", NbtHelper.fromUuid(playerId));

        for (Map.Entry<Component<?, PlayerDataEntry>, ComponentInstance> entry : components.entrySet()) {
            var tagName = entry.getKey().id().toString();

            try {
                tag.put(tagName, entry.getValue().toTag(new NbtCompound()));
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
