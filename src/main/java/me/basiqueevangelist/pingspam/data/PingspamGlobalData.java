package me.basiqueevangelist.pingspam.data;

import me.basiqueevangelist.onedatastore.api.ComponentInstance;
import me.basiqueevangelist.onedatastore.api.DataStore;
import me.basiqueevangelist.pingspam.PingSpam;
import me.basiqueevangelist.pingspam.utils.CaseInsensitiveUtil;
import me.basiqueevangelist.pingspam.utils.OfflineUtil;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PingspamGlobalData implements ComponentInstance {
    private final static Logger LOGGER = LoggerFactory.getLogger("Pingspam/PingspamGlobalData");
    private final Map<String, List<UUID>> groups = CaseInsensitiveUtil.mapIgnoringCase();
    private final DataStore store;

    public PingspamGlobalData(DataStore store) {
        this.store = store;
    }

    @Override
    public void wasMissing() {
        LOGGER.info("Starting import of old Pingspam data");

        for (UUID playerId : OfflineUtil.listSavedPlayers()) {
            LOGGER.info("Importing {}", playerId);
            try {
                NbtCompound tag = OfflineUtil.get(playerId);
                PingspamPlayerData data = store.getPlayer(playerId, PingSpam.PLAYER_DATA);

                if (tag.contains("UnreadPings")) {
                    NbtList pingsTag = tag.getList("UnreadPings", 8);
                    for (NbtElement pingTag : pingsTag) {
                        data.unreadPings().add(Text.Serializer.fromJson(pingTag.asString()));
                    }
                }

                if (tag.contains("Shortnames")) {
                    NbtList aliasesTag = tag.getList("Shortnames", 8);
                    for (NbtElement aliasTag : aliasesTag) {
                        data.aliases().add(aliasTag.asString());
                    }
                }

                if (tag.contains("PingGroups")) {
                    NbtList pingGroupsTag = tag.getList("PingGroups", 8);
                    for (NbtElement pingGroupTag : pingGroupsTag) {
                        addPlayerToGroup(pingGroupTag.asString(), playerId);
                    }
                }

                if (tag.contains("IgnoredPlayers")) {
                    NbtList ignoredPlayerListTag = tag.getList("IgnoredPlayers", NbtElement.INT_ARRAY_TYPE);
                    for (NbtElement ignoredPlayerTag : ignoredPlayerListTag) {
                        data.ignoredPlayers().add(NbtHelper.toUuid(ignoredPlayerTag));
                    }
                }

                if (tag.contains("PingSound")) {
                    if (tag.getString("PingSound").equals("null")) {
                        data.setPingSound(null);
                    } else {
                        data.setPingSound(Registries.SOUND_EVENT.getOrEmpty(new Identifier(tag.getString("PingSound"))).orElse(SoundEvents.BLOCK_BELL_USE));
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Error while reading data for {}", playerId, e);
            }
        }

        LOGGER.info("Finished import of old Pingspam data");
    }

    @Override
    public void fromTag(NbtCompound tag) {
        var groupsTag = tag.getCompound("Groups");
        for (String groupName : groupsTag.getKeys()) {
            var groupTag = groupsTag.getList(groupName, NbtElement.INT_ARRAY_TYPE);
            groups.put(groupName, new ArrayList<>());

            for (NbtElement playerTag : groupTag) {
                addPlayerToGroup(groupName, NbtHelper.toUuid(playerTag));
            }
        }
    }

    public Map<String, List<UUID>> groups() {
        return groups;
    }

    public void addPlayerToGroup(String group, UUID playerId) {
        groups.computeIfAbsent(group, unused -> new ArrayList<>()).add(playerId);
        store.getPlayer(playerId, PingSpam.PLAYER_DATA).groups().add(group);
    }

    public void removePlayerFromGroup(String group, UUID playerId) {
        store.getPlayer(playerId, PingSpam.PLAYER_DATA).groups().remove(group);

        List<UUID> playersInGroup = groups.get(group);

        if (playersInGroup == null) return;

        playersInGroup.remove(playerId);

        if (playersInGroup.size() == 0) groups.remove(group);
    }

    @Override
    public NbtCompound toTag(NbtCompound tag) {
        var groupsTag = new NbtCompound();
        tag.put("Groups", groupsTag);
        for (var entry : groups.entrySet()) {
            var groupTag = new NbtList();
            groupsTag.put(entry.getKey(), groupTag);

            for (UUID playerId : entry.getValue()) {
                groupTag.add(NbtHelper.fromUuid(playerId));
            }
        }

        return tag;
    }
}
