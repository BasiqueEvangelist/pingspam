package me.basiqueevangelist.pingspam.integration;

import me.basiqueevangelist.nevseti.OfflineDataCache;
import me.basiqueevangelist.nevseti.nbt.CompoundTagView;
import me.basiqueevangelist.nevseti.nbt.ListTagView;
import me.basiqueevangelist.pingspam.PingSpam;
import me.basiqueevangelist.pingspam.utils.PlayerList;
import me.basiqueevangelist.pingspam.utils.PlayerUtils;
import me.basiqueevangelist.regrouped.GroupSource;
import me.basiqueevangelist.regrouped.PlayerGroup;
import me.basiqueevangelist.regrouped.Regrouped;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;

public class PingspamGroupSource implements GroupSource {
    public static final PingspamGroupSource INSTANCE = new PingspamGroupSource();

    public void register() {
        Regrouped.registerSource(this);
    }

    @Override
    public @Nullable PlayerGroup findGroupByName(String name) {
        PlayerList list = PlayerUtils.queryPingGroup(PingSpam.SERVER.getPlayerManager(), name);
        if (list.isEmpty())
            return null;

        return new PingspamGroup(name);
    }

    @Override
    public void visitAllGroups(Consumer<PlayerGroup> visitor) {
        PlayerManager manager = PingSpam.SERVER.getPlayerManager();
        Set<String> foundGroups = new HashSet<>();
        for (ServerPlayerEntity onlinePlayer : manager.getPlayerList()) {
            for (String group : PlayerUtils.getPingGroupsOf(onlinePlayer)) {
                if (!foundGroups.contains(group)) {
                    visitor.accept(new PingspamGroup(group));
                    foundGroups.add(group);
                }
            }
        }

        for (Map.Entry<UUID, CompoundTagView> offlineTag : OfflineDataCache.INSTANCE.getPlayers().entrySet()) {
            if (manager.getPlayer(offlineTag.getKey()) != null)
                continue;

            if (offlineTag.getValue().contains("PingGroups")) {
                ListTagView pingGroupsTag = offlineTag.getValue().getList("PingGroups", 8);
                for (int i = 0; i < pingGroupsTag.size(); i++) {
                    String group = pingGroupsTag.getString(i);
                    if (!foundGroups.contains(group)) {
                        visitor.accept(new PingspamGroup(group));
                        foundGroups.add(group);
                    }
                }
            }
        }
    }

    @Override
    public void visitGroupsOf(UUID playerId, Consumer<PlayerGroup> visitor) {
        PlayerManager manager = PingSpam.SERVER.getPlayerManager();
        ServerPlayerEntity onlinePlayer = manager.getPlayer(playerId);
        if (onlinePlayer != null) {
            List<String> groups = PlayerUtils.getPingGroupsOf(onlinePlayer);
            groups.forEach(group -> visitor.accept(new PingspamGroup(group)));
        }
        else {
            CompoundTagView tag = OfflineDataCache.INSTANCE.get(playerId);
            if (tag.contains("PingGroups")) {
                ListTagView groupsTag = tag.getList("PingGroups", 8);
                for (int i = 0; i < groupsTag.size(); i++) {
                    visitor.accept(new PingspamGroup(groupsTag.getString(i)));
                }
            }
        }
    }

    @Override
    public String toString() {
        return "PingspamGroupSource";
    }
}
