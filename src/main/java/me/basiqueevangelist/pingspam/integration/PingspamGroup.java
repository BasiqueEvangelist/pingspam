package me.basiqueevangelist.pingspam.integration;

import me.basiqueevangelist.nevseti.OfflineDataCache;
import me.basiqueevangelist.pingspam.PingSpam;
import me.basiqueevangelist.pingspam.utils.PlayerList;
import me.basiqueevangelist.pingspam.utils.PlayerUtils;
import me.basiqueevangelist.regrouped.GroupSource;
import me.basiqueevangelist.regrouped.PlayerGroup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class PingspamGroup implements PlayerGroup {
    private final String name;

    public PingspamGroup(String name) {
        this.name = name;
    }

    @Override
    public List<UUID> getMembers() {
        PlayerList list = PlayerUtils.queryPingGroup(PingSpam.SERVER.getPlayerManager(), name);
        List<UUID> uuids = new ArrayList<>(list.getOfflinePlayers());
        for (ServerPlayerEntity player : list.getOnlinePlayers()) {
            uuids.add(player.getUuid());
        }
        return Collections.unmodifiableList(uuids);
    }

    @Override
    public boolean canChangeMembers() {
        return true;
    }

    @Override
    public void addMember(UUID uuid) {
        PlayerManager manager = PingSpam.SERVER.getPlayerManager();
        ServerPlayerEntity onlinePlayer = manager.getPlayer(uuid);
        if (onlinePlayer != null) {
            List<String> groups = PlayerUtils.getPingGroupsOf(onlinePlayer);
            if (!groups.contains(name))
                groups.add(name);
        } else {
            CompoundTag tag = OfflineDataCache.INSTANCE.reload(uuid).copy();
            if (!tag.contains("PingGroups"))
                tag.put("PingGroups", new ListTag());
            ListTag groupsTag = tag.getList("PingGroups", 8);
            for (Tag groupTag : groupsTag) {
                if (groupTag.asString().equals(name))
                    return;
            }
            groupsTag.add(StringTag.of(name));
            OfflineDataCache.INSTANCE.save(uuid, tag);
        }
    }

    @Override
    public void removeMember(UUID uuid) {
        PlayerManager manager = PingSpam.SERVER.getPlayerManager();
        ServerPlayerEntity onlinePlayer = manager.getPlayer(uuid);
        if (onlinePlayer != null) {
            List<String> groups = PlayerUtils.getPingGroupsOf(onlinePlayer);
            groups.remove(name);
        } else {
            CompoundTag tag = OfflineDataCache.INSTANCE.reload(uuid).copy();
            if (!tag.contains("PingGroups"))
                tag.put("PingGroups", new ListTag());
            ListTag groupsTag = tag.getList("PingGroups", 8);
            groupsTag.removeIf(groupTag -> groupTag.asString().equals(name));
            OfflineDataCache.INSTANCE.save(uuid, tag);
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public GroupSource getSource() {
        return PingspamGroupSource.INSTANCE;
    }
}
