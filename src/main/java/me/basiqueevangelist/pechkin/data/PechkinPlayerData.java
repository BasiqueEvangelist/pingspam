package me.basiqueevangelist.pechkin.data;

import me.basiqueevangelist.onedatastore.api.ComponentInstance;
import me.basiqueevangelist.onedatastore.api.PlayerDataEntry;
import me.basiqueevangelist.pechkin.Pechkin;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record PechkinPlayerData(
    List<MailMessage> messages,
    List<UUID> ignoredPlayers,
    List<UUID> lastCorrespondents,
    LeakyBucket leakyBucket
) implements ComponentInstance {
    public PechkinPlayerData(PlayerDataEntry ignored) {
        this(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new LeakyBucket());
    }

    public void fromTag(NbtCompound tag, RegistryWrapper.WrapperLookup registries) {
        var messagesTag = tag.getList("Messages", NbtElement.COMPOUND_TYPE);

        for (int i = 0; i < messagesTag.size(); i++) {
            messages.add(MailMessage.fromTag(messagesTag.getCompound(i), registries));
        }

        var ignoredPlayersTag = tag.getList("IgnoredPlayers", NbtElement.INT_ARRAY_TYPE);

        for (var ignoredPlayerTag : ignoredPlayersTag) {
            ignoredPlayers.add(NbtHelper.toUuid(ignoredPlayerTag));
        }

        var lastCorrespondentsTag = tag.getList("LastCorrespondents", NbtElement.INT_ARRAY_TYPE);

        for (var correspondentTag : lastCorrespondentsTag) {
            lastCorrespondents.add(NbtHelper.toUuid(correspondentTag));
        }

        leakyBucket.fromTag(tag);
    }

    public NbtCompound toTag(NbtCompound tag, RegistryWrapper.WrapperLookup registries) {
        if (!messages.isEmpty()) {
            var messagesTag = new NbtList();
            tag.put("Messages", messagesTag);
            for (var message : messages) {
                messagesTag.add(message.toTag(new NbtCompound(), registries));
            }
        }

        if (!ignoredPlayers.isEmpty()) {
            var ignoresTag = new NbtList();
            tag.put("IgnoredPlayers", ignoresTag);
            for (var ignoredPlayer : ignoredPlayers) {
                ignoresTag.add(NbtHelper.fromUuid(ignoredPlayer));
            }
        }

        if (!lastCorrespondents.isEmpty()) {
            var lastCorrespondentsTag = new NbtList();
            tag.put("LastCorrespondents", lastCorrespondentsTag);
            for (var correspondent : lastCorrespondents) {
                lastCorrespondentsTag.add(NbtHelper.fromUuid(correspondent));
            }
        }

        leakyBucket.toTag(tag);

        return tag;
    }

    public void addCorrespondent(UUID id) {
        int maxCorrespondents = Pechkin.CONFIG.getConfig().maxCorrespondents;

        if (!lastCorrespondents.contains(id)) {
            if (lastCorrespondents.size() >= maxCorrespondents)
                lastCorrespondents.remove(maxCorrespondents - 1);
            lastCorrespondents.add(0, id);
        } else {
            int index = lastCorrespondents.indexOf(id);
            if (index != 0) {
                lastCorrespondents.remove(index);
                lastCorrespondents.add(0, id);
            }
        }
    }

    public void addMessage(MailMessage msg) {
        int maxMessages = Pechkin.CONFIG.getConfig().maxInboxMessages;

        if (messages.size() >= maxMessages)
            messages.remove(maxMessages - 1);
        messages.add(0, msg);
    }
}
