package me.basiqueevangelist.pingspam.data;

import me.basiqueevangelist.onedatastore.api.ComponentInstance;
import me.basiqueevangelist.pingspam.utils.CaseInsensitiveUtil;
import net.minecraft.nbt.*;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class PingspamPlayerData implements ComponentInstance {
    private final List<Text> unreadPings;
    private final Set<String> aliases;
    private final List<UUID> ignoredPlayers;
    private @Nullable SoundEvent pingSound;
    private final Set<String> groups;

    public PingspamPlayerData(
        List<Text> unreadPings,
        Set<String> aliases,
        List<UUID> ignoredPlayers,
        @Nullable SoundEvent pingSound,
        Set<String> groups
    ) {
        this.unreadPings = unreadPings;
        this.aliases = aliases;
        this.ignoredPlayers = ignoredPlayers;
        this.pingSound = pingSound;
        this.groups = groups;
    }

    public PingspamPlayerData() {
        this(new ArrayList<>(), CaseInsensitiveUtil.setIgnoringCase(), new ArrayList<>(), SoundEvents.BLOCK_BELL_USE, CaseInsensitiveUtil.setIgnoringCase());
    }

    @Override
    public void fromTag(NbtCompound tag) {
        if (tag.contains("UnreadPings")) {
            NbtList pingsTag = tag.getList("UnreadPings", NbtElement.STRING_TYPE);
            for (NbtElement pingTag : pingsTag) {
                unreadPings.add(Text.Serializer.fromJson(pingTag.asString()));
            }
        }

        if (tag.contains("Aliases")) {
            NbtList aliasesTag = tag.getList("Aliases", NbtElement.STRING_TYPE);
            for (NbtElement aliasTag : aliasesTag) {
                aliases.add(aliasTag.asString());
            }
        }

        if (tag.contains("IgnoredPlayers")) {
            NbtList ignoredPlayerListTag = tag.getList("IgnoredPlayers", NbtElement.INT_ARRAY_TYPE);
            for (NbtElement ignoredPlayerTag : ignoredPlayerListTag) {
                ignoredPlayers.add(NbtHelper.toUuid(ignoredPlayerTag));
            }
        }

        if (tag.contains("PingSound", NbtElement.STRING_TYPE)) {
            var soundText = tag.getString("PingSound");
            if (soundText.equals("null")) {
                pingSound = null;
            } else {
                pingSound = Registries.SOUND_EVENT.getOrEmpty(new Identifier(soundText)).orElse(SoundEvents.BLOCK_BELL_USE);
            }
        }
    }

    @Override
    public NbtCompound toTag(NbtCompound tag) {
        if (!unreadPings.isEmpty()) {
            var unreadPingsTag = new NbtList();
            tag.put("UnreadPings", unreadPingsTag);
            for (var unreadPing : unreadPings) {
                unreadPingsTag.add(NbtString.of(Text.Serializer.toJson(unreadPing)));
            }
        }

        if (!aliases.isEmpty()) {
            var aliasesTag = new NbtList();
            tag.put("Aliases", aliasesTag);
            for (var alias : aliases) {
                aliasesTag.add(NbtString.of(alias));
            }
        }

        if (!ignoredPlayers.isEmpty()) {
            var ignoresTag = new NbtList();
            tag.put("IgnoredPlayers", ignoresTag);
            for (var ignoredPlayer : ignoredPlayers) {
                ignoresTag.add(NbtHelper.fromUuid(ignoredPlayer));
            }
        }

        if (pingSound == null) {
            tag.putString("PingSound", "null");
        } else if (pingSound != SoundEvents.BLOCK_BELL_USE) {
            tag.putString("PingSound", pingSound.getId().toString());
        }

        return tag;
    }

    public void addPing(Text pingMsg) {
        while (unreadPings.size() >= 100)
            unreadPings.remove(0);
        unreadPings.add(pingMsg);
    }

    public List<Text> unreadPings() {
        return unreadPings;
    }

    public Set<String> aliases() {
        return aliases;
    }

    public List<UUID> ignoredPlayers() {
        return ignoredPlayers;
    }

    public @Nullable SoundEvent pingSound() {
        return pingSound;
    }

    public Set<String> groups() {
        return groups;
    }

    public void setPingSound(@Nullable SoundEvent pingSound) {
        this.pingSound = pingSound;
    }
}
