package me.basiqueevangelist.pingspam.logic;

import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.command.CommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

@SuppressWarnings("BooleanMethodIsAlwaysInverted")
public final class PingspamPermissions {
    private PingspamPermissions() { }

    //region /pingspam alias
    public static boolean addOwnAlias(CommandSource source) {
        return Permissions.check(source, "pingspam.alias.own.add", true);
    }

    public static boolean removeOwnAlias(CommandSource source) {
        return Permissions.check(source, "pingspam.alias.own.remove", true);
    }

    public static boolean addPlayerAlias(CommandSource source) {
        return Permissions.check(source, "pingspam.alias.player.add", 2);
    }

    public static boolean removePlayerAlias(CommandSource source) {
        return Permissions.check(source, "pingspam.alias.player.remove", 2);
    }
    //endregion

    //region /pingspam group
    public static boolean addGroupPlayer(CommandSource source) {
        return Permissions.check(source, "pingspam.group.player.add", 2);
    }

    public static boolean configureGroup(CommandSource source) {
        return Permissions.check(source, "pingspam.group.configure", 2);
    }
    //endregion

    //region bypass*
    public static boolean bypassAliasLimit(CommandSource source) {
        return Permissions.check(source, "pingspam.bypass.aliaslimit", 2);
    }

    public static boolean bypassIgnore(ServerPlayerEntity source) {
        return Permissions.check(source, "pingspam.bypass.ignore", 2);
    }

    public static boolean bypassCooldown(ServerPlayerEntity source) {
        return Permissions.check(source, "pechkin.bypass.cooldown", 2);
    }
    //endregion

    //region ping*
    public static boolean pingEveryone(ServerPlayerEntity source) {
        return Permissions.check(source, "pingspam.ping.everyone", 2);
    }

    public static boolean pingOnline(ServerPlayerEntity source) {
        return Permissions.check(source, "pingspam.ping.online", 2);
    }

    public static boolean pingOffline(ServerPlayerEntity source) {
        return Permissions.check(source, "pingspam.ping.offline", 2);
    }

    public static boolean pingPlayer(ServerPlayerEntity source) {
        return Permissions.check(source, "pingspam.ping.player", true);
    }

    public static boolean pingGroup(ServerPlayerEntity source) {
        return Permissions.check(source, "pingspam.ping.group", true);
    }
    //endregion

    //region /mail
    public static boolean sendMail(CommandSource source) {
        return Permissions.check(source, "pechkin.send", true);
    }

    public static boolean clearOtherMail(CommandSource source) {
        return Permissions.check(source, "pechkin.clear.other", 2);
    }

    public static boolean listOtherMail(CommandSource source) {
        return Permissions.check(source, "pechkin.list.other", 2);
    }

    public static boolean deleteOtherMail(CommandSource source) {
        return Permissions.check(source, "pechkin.delete.other", 2);
    }
    //endregion

    public static boolean purge(CommandSource source) {
        return Permissions.check(source, "onedatastore.purge", 4);
    }

    public static boolean reload(CommandSource source) {
        return Permissions.check(source, "pingspam.reload", 2);
    }
}
