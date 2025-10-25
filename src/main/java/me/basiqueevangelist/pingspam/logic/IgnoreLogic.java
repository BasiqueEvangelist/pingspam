package me.basiqueevangelist.pingspam.logic;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import me.basiqueevangelist.onedatastore.api.DataStore;
import me.basiqueevangelist.pingspam.PingSpam;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.UUID;

public final class IgnoreLogic {
    private static final SimpleCommandExceptionType IGNORED = new SimpleCommandExceptionType(Text.literal("That player is ignoring you."));

    private IgnoreLogic() {

    }

    public static boolean isIgnored(ServerPlayerEntity player, UUID by) {
        if (PingspamPermissions.bypassIgnore(player)) return false;

        return DataStore.getFor(player.getEntityWorld().getServer()).getPlayer(by, PingSpam.PLAYER_DATA).ignoredPlayers().contains(player.getUuid());
    }

    public static void throwIfIgnored(ServerPlayerEntity player, UUID by) throws CommandSyntaxException {
        if (isIgnored(player, by)) {
            throw IGNORED.create();
        }
    }
}
