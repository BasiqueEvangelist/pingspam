package me.basiqueevangelist.onedatastore.impl;

import me.basiqueevangelist.onedatastore.api.Component;
import me.basiqueevangelist.onedatastore.api.ComponentInstance;
import me.basiqueevangelist.onedatastore.api.DataStore;
import me.basiqueevangelist.onedatastore.api.PlayerDataEntry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;

import java.util.*;

public class OneDataStoreState extends PersistentState implements DataStore {
    private final Map<UUID, PlayerDataEntryImpl> players = new HashMap<>();
    private final Map<Component<?, DataStore>, ComponentInstance> components = new HashMap<>();
    private static final ReentrantLoadProtector SAFEGUARD = new ReentrantLoadProtector(() -> new IllegalStateException("Tried to recursively load OneDataStore state!"));

    public static OneDataStoreState getFrom(MinecraftServer server) {
        try (var scope = SAFEGUARD.enter()) {
            return server.getOverworld().getPersistentStateManager().getOrCreate(
                OneDataStoreState::new,
                OneDataStoreState::new,
                "onedatastore"
            );
        }
    }

    private OneDataStoreState() {
        for (Component<?, DataStore> comp : OneDataStoreInit.GLOBAL_COMPONENTS.values()) {
            ComponentInstance inst = comp.factory().apply(this);
            inst.wasMissing();
            components.put(comp, inst);
        }
    }

    private OneDataStoreState(NbtCompound tag) {
        var playersTag = tag.getList("Players", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < playersTag.size(); i++) {
            var playerTag = playersTag.getCompound(i);

            UUID playerId = playerTag.getUuid("UUID");

            players.put(playerId, new PlayerDataEntryImpl(this, playerId));
        }

        for (Component<?, DataStore> comp : OneDataStoreInit.GLOBAL_COMPONENTS.values()) {
            components.put(comp, comp.factory().apply(this));
        }

        for (int i = 0; i < playersTag.size(); i++) {
            var playerTag = playersTag.getCompound(i);

            UUID playerId = playerTag.getUuid("UUID");

            players.get(playerId).fromTag(playerTag);
        }

        for (Map.Entry<Component<?, DataStore>, ComponentInstance> entry : components.entrySet()) {
            var tagName = entry.getKey().id().toString();

            if (tag.contains(tagName, NbtElement.COMPOUND_TYPE)) {
                try {
                    entry.getValue().fromTag(tag.getCompound(tagName));
                } catch (Exception e) {
                    OneDataStoreInit.LOGGER.error("Encountered error while deserializing {}", tagName, e);
                }
            } else {
                entry.getValue().wasMissing();
            }
        }
    }

    @Override
    public PlayerDataEntry getPlayerEntry(UUID playerId) {
        return players.computeIfAbsent(playerId, id -> {
            var entry = new PlayerDataEntryImpl(this, id);
            entry.wasMissing();
            return entry;
        });
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends ComponentInstance> T get(Component<T, DataStore> component) {
        return (T) components.get(component);
    }

    @Override
    public <T extends ComponentInstance> T getPlayer(UUID playerId, Component<T, PlayerDataEntry> component) {
        return getPlayerEntry(playerId).get(component);
    }

    @Override
    public Collection<PlayerDataEntry> players() {
        return Collections.unmodifiableCollection(players.values());
    }

    public Map<UUID, PlayerDataEntryImpl> playersMap() {
        return players;
    }

    public void reinitComponent(Component<?, DataStore> component) {
        components.remove(component);
        ComponentInstance inst = component.factory().apply(this);
        inst.wasMissing();
        components.put(component, inst);
    }

    @Override
    public NbtCompound writeNbt(NbtCompound tag) {
        var playersTag = new NbtList();
        tag.put("Players", playersTag);

        for (var entry : players.entrySet()) {
            playersTag.add(entry.getValue().toTag(new NbtCompound()));
        }

        for (Map.Entry<Component<?, DataStore>, ComponentInstance> entry : components.entrySet()) {
            var tagName = entry.getKey().id().toString();

            try {
                tag.put(tagName, entry.getValue().toTag(new NbtCompound()));
            } catch (Exception e) {
                OneDataStoreInit.LOGGER.error("Encountered error while serializing {}", tagName, e);
            }
        }

        return tag;
    }

    @Override
    public boolean isDirty() {
        return true;
    }
}
