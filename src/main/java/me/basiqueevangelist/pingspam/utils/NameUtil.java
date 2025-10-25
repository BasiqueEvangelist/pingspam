package me.basiqueevangelist.pingspam.utils;

import com.mojang.authlib.yggdrasil.ProfileResult;
import me.basiqueevangelist.pingspam.PingSpam;
import net.minecraft.server.PlayerConfigEntry;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class NameUtil {
    private NameUtil() {

    }

    private static final Set<UUID> BAD_UUIDS = new HashSet<>();

    public static @Nullable String getNameFromUUIDOrNull(UUID uuid) {
        if (BAD_UUIDS.contains(uuid)) return null;

        var optProfile = PingSpam.SERVER.getApiServices().nameToIdCache().getByUuid(uuid);

        if (optProfile.isPresent()) return optProfile.get().name();

        ProfileResult profile = PingSpam.SERVER.getApiServices().sessionService().fetchProfile(uuid, true);

        if (profile != null) {
            PingSpam.SERVER.getApiServices().nameToIdCache().add(new PlayerConfigEntry(profile.profile()));
            return profile.profile().name();
        }

        BAD_UUIDS.add(uuid);

        return null;
    }

    public static String getNameFromUUID(UUID uuid) {
        String name = getNameFromUUIDOrNull(uuid);

        if (name != null)
            return name;
        else
            return "<" + uuid.toString() + ">";
    }
}
