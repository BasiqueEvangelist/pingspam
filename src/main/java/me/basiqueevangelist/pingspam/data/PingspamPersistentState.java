package me.basiqueevangelist.pingspam.data;

import me.basiqueevangelist.pingspam.utils.CaseInsensitiveUtil;
import me.basiqueevangelist.pingspam.utils.OfflineUtil;
import net.fabricmc.fabric.api.util.NbtType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.PersistentState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class PingspamPersistentState extends PersistentState {
    private final static Logger LOGGER = LoggerFactory.getLogger("Pingspam/PingspamPersistentState");

    private final Map<UUID, PingspamPlayerData> playerMap = new HashMap<>();
    private final Map<String, List<UUID>> groups = CaseInsensitiveUtil.mapIgnoringCase();

    public static PingspamPersistentState getFrom(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager().getOrCreate(
            PingspamPersistentState::new,
            PingspamPersistentState::new,
            "pingspam"
        );
    }

    public PingspamPersistentState() {
        LOGGER.info("Starting import of old Pingspam data");

        for (UUID playerId : OfflineUtil.listSavedPlayers()) {
            LOGGER.info("Importing {}", playerId);
            try {
                NbtCompound tag = OfflineUtil.get(playerId);
                PingspamPlayerData data = getFor(playerId);

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
                    NbtList ignoredPlayerListTag = tag.getList("IgnoredPlayers", NbtType.INT_ARRAY);
                    for (NbtElement ignoredPlayerTag : ignoredPlayerListTag) {
                        data.ignoredPlayers().add(NbtHelper.toUuid(ignoredPlayerTag));
                    }
                }

                if (tag.contains("PingSound")) {
                    if (tag.getString("PingSound").equals("null")) {
                        data.setPingSound(null);
                    } else {
                        data.setPingSound(Registry.SOUND_EVENT.getOrEmpty(new Identifier(tag.getString("PingSound"))).orElse(SoundEvents.BLOCK_BELL_USE));
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Error while reading data for {}", playerId, e);
            }
        }

        LOGGER.info("Finished import of old Pingspam data");
    }

    public PingspamPersistentState(NbtCompound tag) {
        var playersTag = tag.getList("Players", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < playersTag.size(); i++) {
            var playerTag = playersTag.getCompound(i);

            UUID playerId = playerTag.getUuid("UUID");

            playerMap.put(playerId, PingspamPlayerData.fromTag(playerTag));
        }

        var groupsTag = tag.getCompound("Groups");
        for (String groupName : groupsTag.getKeys()) {
            var groupTag = groupsTag.getList(groupName, NbtElement.INT_ARRAY_TYPE);
            groups.put(groupName, new ArrayList<>());

            for (NbtElement playerTag : groupTag) {
                addPlayerToGroup(groupName, NbtHelper.toUuid(playerTag));
            }
        }
    }

    public PingspamPlayerData getFor(UUID id) {
        return playerMap.computeIfAbsent(id, _unused -> new PingspamPlayerData());
    }

    public Map<UUID, PingspamPlayerData> getPlayerMap() {
        return playerMap;
    }

    public Map<String, List<UUID>> getGroups() {
        return groups;
    }

    public void addPlayerToGroup(String group, UUID playerId) {
        groups.computeIfAbsent(group, unused -> new ArrayList<>()).add(playerId);
        getFor(playerId).groups().add(group);
    }

    public void removePlayerFromGroup(String group, UUID playerId) {
        getFor(playerId).groups().remove(group);

        List<UUID> playersInGroup = groups.get(group);

        if (playersInGroup == null) return;

        playersInGroup.remove(playerId);

        if (playersInGroup.size() == 0) groups.remove(group);
    }

    @Override
    public NbtCompound writeNbt(NbtCompound tag) {
        var playersTag = new NbtList();
        tag.put("Players", playersTag);

        for (var entry : playerMap.entrySet()) {
            var playerTag = entry.getValue().toTag(new NbtCompound());
            playerTag.put("UUID", NbtHelper.fromUuid(entry.getKey()));
            playersTag.add(playerTag);
        }

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

    @Override
    public boolean isDirty() {
        return true;
    }
}
