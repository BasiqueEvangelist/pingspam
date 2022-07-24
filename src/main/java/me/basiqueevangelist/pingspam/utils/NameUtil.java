package me.basiqueevangelist.pingspam.utils;

import com.mojang.authlib.GameProfile;
import me.basiqueevangelist.pingspam.PingSpam;
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

        var optProfile = PingSpam.SERVER.getUserCache().getByUuid(uuid);

        if (optProfile.isPresent()) return optProfile.get().getName();

        GameProfile profile = PingSpam.SERVER.getSessionService().fillProfileProperties(new GameProfile(uuid, null), true);

        if (profile.getName() != null) {
            PingSpam.SERVER.getUserCache().add(profile);
            return profile.getName();
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
