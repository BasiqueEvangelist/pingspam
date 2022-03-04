package me.basiqueevangelist.pingspam.utils;

import com.mojang.authlib.GameProfile;
import me.basiqueevangelist.pingspam.PingSpam;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class NameUtil {
    private NameUtil() {

    }

    public static @Nullable String getNameFromUUIDOrNull(UUID uuid) {
        var optProfile = PingSpam.SERVER.getUserCache().getByUuid(uuid);

        if (optProfile.isPresent()) return optProfile.get().getName();

        GameProfile profile = new GameProfile(uuid, null);
        PingSpam.SERVER.getSessionService().fillProfileProperties(profile, true);

        if (profile.getName() != null) return profile.getName();

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
