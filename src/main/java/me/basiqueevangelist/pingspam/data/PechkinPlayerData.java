package me.basiqueevangelist.pingspam.data;

import me.basiqueevangelist.onedatastore.api.ComponentInstance;
import me.basiqueevangelist.onedatastore.api.PlayerDataEntry;
import me.basiqueevangelist.pingspam.PingSpam;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Uuids;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record PechkinPlayerData(
    List<MailMessage> messages,
    List<UUID> lastCorrespondents,
    LeakyBucket leakyBucket
) implements ComponentInstance {
    public PechkinPlayerData(PlayerDataEntry ignored) {
        this(new ArrayList<>(), new ArrayList<>(), new LeakyBucket());
    }

    public void fromTag(NbtCompound tag, RegistryWrapper.WrapperLookup registries) {
        var messagesTag = tag.get("Messages", MailMessage.CODEC.listOf()).orElse(List.of());
        messages.addAll(messagesTag);

        var lastCorrespondentsTag = tag.get("LastCorrespondents", Uuids.CODEC.listOf()).orElse(List.of());
        lastCorrespondents.addAll(lastCorrespondentsTag);

        leakyBucket.fromTag(tag);
    }

    public NbtCompound toTag(NbtCompound tag, RegistryWrapper.WrapperLookup registries) {
        if (!messages.isEmpty()) {
            tag.put("Messages", MailMessage.CODEC.listOf(), messages);
        }

        if (!lastCorrespondents.isEmpty()) {
            tag.put("LastCorrespondents", Uuids.CODEC.listOf(), lastCorrespondents);
        }

        leakyBucket.toTag(tag);

        return tag;
    }

    public void addCorrespondent(UUID id) {
        int maxCorrespondents = PingSpam.CONFIG.getConfig().mail.maxCorrespondents;

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
        int maxMessages = PingSpam.CONFIG.getConfig().mail.maxInboxMessages;

        if (messages.size() >= maxMessages)
            messages.remove(maxMessages - 1);
        messages.add(0, msg);
    }
}
