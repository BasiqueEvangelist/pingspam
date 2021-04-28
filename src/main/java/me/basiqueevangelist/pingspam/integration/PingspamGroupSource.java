package me.basiqueevangelist.pingspam.integration;

import me.basiqueevangelist.nevseti.OfflineDataCache;
import me.basiqueevangelist.nevseti.OfflineDataChanged;
import me.basiqueevangelist.nevseti.OfflineDataLoaded;
import me.basiqueevangelist.nevseti.nbt.CompoundTagView;
import me.basiqueevangelist.nevseti.nbt.ListTagView;
import me.basiqueevangelist.pingspam.PingSpam;
import me.basiqueevangelist.pingspam.utils.PlayerList;
import me.basiqueevangelist.pingspam.utils.PlayerUtils;
import me.basiqueevangelist.regrouped.GroupSource;
import me.basiqueevangelist.regrouped.PlayerGroup;
import me.basiqueevangelist.regrouped.Regrouped;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.util.NbtType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;

public class PingspamGroupSource implements GroupSource {
    public static final PingspamGroupSource INSTANCE = new PingspamGroupSource();
    private final Map<String, PingspamGroup> groups = new HashMap<>();

    public void register() {
        Regrouped.registerSource(this);
        OfflineDataLoaded.EVENT.register(this::onOfflineDataLoaded);
        ServerLifecycleEvents.SERVER_STOPPED.register(this::onServerStopped);

        OfflineDataChanged.EVENT.register(this::onOfflineDataChanged);
    }

    private void onOfflineDataChanged(UUID playerId, CompoundTagView tag) {
        if (tag.contains("PingGroups")) {
            ListTagView groupsTag = tag.getList("PingGroups", NbtType.STRING);
            List<String> groupsFound = new ArrayList<>();
            for (int i = 0; i < groupsTag.size(); i++) {
                groupsFound.add(groupsTag.getString(i));
            }
            for (PingspamGroup group : groups.values()) {
                if (group.getMembersInternal().contains(playerId) && !groupsFound.contains(group.getName())) {
                    group.getMembersInternal().remove(playerId);
                }

                if (!group.getMembersInternal().contains(playerId) && groupsFound.contains(group.getName())) {
                    group.getMembersInternal().add(playerId);
                }
            }
        }
    }

    private void onOfflineDataLoaded() {
        for (Map.Entry<UUID, CompoundTagView> data : OfflineDataCache.INSTANCE.getPlayers().entrySet()) {
            UUID id = data.getKey();
            CompoundTagView tag = data.getValue();

            if (tag.contains("PingGroups")) {
                ListTagView groupsTag = tag.getList("PingGroups", NbtType.STRING);
                for (int i = 0; i < groupsTag.size(); i++) {
                    String groupName = groupsTag.getString(i);

                    PingspamGroup group = groups.computeIfAbsent(groupName, PingspamGroup::new);
                    group.getMembersInternal().add(id);
                }
            }
        }
    }

    private void onServerStopped(MinecraftServer server) {
        groups.clear();
    }

    public PingspamGroup getOrCreateGroup(String name) {
        return groups.computeIfAbsent(name, PingspamGroup::new);
    }

    @Override
    public @Nullable PingspamGroup findGroupByName(String name) {
        PingspamGroup group = groups.get(name);
        if (group != null && group.getMembersInternal().size() == 0) {
            groups.remove(name);
            return null;
        }
        return group;
    }

    @Override
    public void visitAllGroups(Consumer<PlayerGroup> visitor) {
        Iterator<PingspamGroup> iterator = groups.values().iterator();
        while (iterator.hasNext()) {
            PingspamGroup group = iterator.next();
            if (group.getMembersInternal().size() == 0)
                iterator.remove();
            else
                visitor.accept(group);
        }
    }

    public List<PlayerGroup> getAllGroups() {
        List<PlayerGroup> groups = new ArrayList<>();
        visitAllGroups(groups::add);
        return groups;
    }

    public List<PlayerGroup> getGroupsOf(UUID player) {
        List<PlayerGroup> groups = new ArrayList<>();
        visitGroupsOf(player, groups::add);
        return groups;
    }

    @Override
    public void visitGroupsOf(UUID playerId, Consumer<PlayerGroup> visitor) {
        PlayerManager manager = PingSpam.SERVER.getPlayerManager();
        ServerPlayerEntity onlinePlayer = manager.getPlayer(playerId);
        if (onlinePlayer != null) {
            List<String> groups = PlayerUtils.getPingGroupsOf(onlinePlayer);
            groups.forEach(this::findGroupByName);
        }
        else {
            CompoundTagView tag = OfflineDataCache.INSTANCE.get(playerId);
            if (tag.contains("PingGroups")) {
                ListTagView groupsTag = tag.getList("PingGroups", 8);
                for (int i = 0; i < groupsTag.size(); i++) {
                    visitor.accept(findGroupByName(groupsTag.getString(i)));
                }
            }
        }
    }

    @Override
    public String toString() {
        return "PingspamGroupSource";
    }
}
