package me.basiqueevangelist.pingspam.data;

import com.mojang.serialization.Codec;
import me.basiqueevangelist.onedatastore.api.ComponentInstance;
import me.basiqueevangelist.onedatastore.api.DataStore;
import me.basiqueevangelist.pingspam.PingSpam;
import me.basiqueevangelist.pingspam.utils.CaseInsensitiveUtil;
import me.basiqueevangelist.pingspam.utils.CodecUtil;
import me.basiqueevangelist.pingspam.utils.OfflineUtil;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PingspamGlobalData implements ComponentInstance {
    private final static Logger LOGGER = LoggerFactory.getLogger("Pingspam/PingspamGlobalData");
    private final Map<String, PingspamGroupData> groups = CaseInsensitiveUtil.mapIgnoringCase();
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
                    List<Text> pings = tag.get("UnreadPings", CodecUtil.TEXT_JSON.listOf()).orElse(List.of());
                    data.unreadPings().addAll(pings);
                }

                if (tag.contains("Shortnames")) {
                    List<String> aliases = tag.get("Shortnames", Codec.STRING.listOf()).orElse(List.of());
                    data.aliases().addAll(aliases);
                }

                if (tag.contains("PingGroups")) {
                    List<String> pingGroups = tag.get("PingGroups", Codec.STRING.listOf()).orElse(List.of());
                    for (String pingGroup : pingGroups) {
                        addPlayerToGroup(pingGroup, playerId);
                    }
                }

                if (tag.contains("IgnoredPlayers")) {
                    List<UUID> ignoredPlayerList = tag.get("IgnoredPlayers", Uuids.CODEC.listOf()).orElse(List.of());
                    data.ignoredPlayers().addAll(ignoredPlayerList);
                }

                if (tag.contains("PingSound")) {
                    if (tag.getString("PingSound").orElse("").equals("null")) {
                        data.setPingSound(null);
                    } else {
                        data.setPingSound(Registries.SOUND_EVENT.getOptionalValue(Identifier.of(tag.getString("PingSound").orElseThrow())).orElse(SoundEvents.BLOCK_BELL_USE));
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Error while reading data for {}", playerId, e);
            }
        }

        LOGGER.info("Finished import of old Pingspam data");
    }

    @Override
    public void fromTag(NbtCompound tag, RegistryWrapper.WrapperLookup registries) {
        var groupsTag = tag.getCompoundOrEmpty("Groups");
        for (String groupName : groupsTag.getKeys()) {
            var group = new PingspamGroupData(groupName);
            groups.put(groupName, group);
            var groupTag = groupsTag.get(groupName);

            if (groupTag instanceof NbtList) {
                throw new UnsupportedOperationException();
            } else if (groupTag instanceof NbtCompound compound) {
                group.fromTag(compound);
            }

            propagateGroup(group);
        }

    }

    public Map<String, PingspamGroupData> groups() {
        return groups;
    }

    private void propagateGroup(PingspamGroupData group) {
        for (var memberId : group.members()) {
            store.getPlayer(memberId, PingSpam.PLAYER_DATA).groups().add(group.name());
        }
    }

    public void addPlayerToGroup(String group, UUID playerId) {
        groups.computeIfAbsent(group, PingspamGroupData::new).members.add(playerId);
        store.getPlayer(playerId, PingSpam.PLAYER_DATA).groups().add(group);
    }

    public void removePlayerFromGroup(String group, UUID playerId) {
        store.getPlayer(playerId, PingSpam.PLAYER_DATA).groups().remove(group);

        PingspamGroupData groupData = groups.get(group);

        if (groupData == null) return;

        groupData.members.remove(playerId);

        if (groupData.members().size() == 0) groups.remove(group);
    }

    @Override
    public NbtCompound toTag(NbtCompound tag, RegistryWrapper.WrapperLookup registries) {
        var groupsTag = new NbtCompound();
        tag.put("Groups", groupsTag);
        for (var entry : groups.entrySet()) {
            groupsTag.put(entry.getKey(), entry.getValue().toTag(new NbtCompound()));
        }

        return tag;
    }
}
