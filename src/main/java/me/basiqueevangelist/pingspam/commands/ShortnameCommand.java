package me.basiqueevangelist.pingspam.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import me.basiqueevangelist.pingspam.OfflinePlayerCache;
import me.basiqueevangelist.pingspam.PlayerUtils;
import me.basiqueevangelist.pingspam.access.ServerPlayerEntityAccess;
import me.basiqueevangelist.pingspam.network.PingSpamPackets;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class ShortnameCommand {
    private static final SimpleCommandExceptionType SHORTNAME_EXISTS = new SimpleCommandExceptionType(new LiteralText("You already have that shortname!"));
    private static final DynamicCommandExceptionType SHORTNAME_EXISTS_OTHER = new DynamicCommandExceptionType(x ->
        ((ServerPlayerEntity) x).getDisplayName().shallowCopy().append(new LiteralText(" already has that shortname!")));
    private static final SimpleCommandExceptionType NO_SUCH_SHORTNAME = new SimpleCommandExceptionType(new LiteralText("You don't have that shortname!"));
    private static final DynamicCommandExceptionType NO_SUCH_SHORTNAME_OTHER = new DynamicCommandExceptionType(x ->
        ((ServerPlayerEntity) x).getDisplayName().shallowCopy().append(new LiteralText(" doesn't have that shortname!")));
    private static final SimpleCommandExceptionType SHORTNAME_COLLISION = new SimpleCommandExceptionType(new LiteralText("Shortname collides with other player's shortname!"));
    private static final SimpleCommandExceptionType INVALID_SHORTNAME = new SimpleCommandExceptionType(new LiteralText("Invalid shortname!"));
    private static final Pattern SHORTNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{2,16}$");

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
            literal("shortname")
                .then(literal("list")
                    .executes(ShortnameCommand::listShortnames))
                .then(literal("add")
                    .requires(x -> Permissions.check(x, "pingspam.addownshortname", 0))
                    .then(argument("shortname", StringArgumentType.word())
                        .executes(ShortnameCommand::addShortname)))
                .then(literal("remove")
                    .requires(x -> Permissions.check(x, "pingspam.removeownshortname", 0))
                    .then(argument("shortname", StringArgumentType.word())
                        .executes(ShortnameCommand::removeShortname)))
                .then(literal("player")
                    .then(argument("player", EntityArgumentType.player())
                        .then(literal("list")
                            .executes(ShortnameCommand::listPlayerShortnames))
                        .then(literal("add")
                            .requires(x -> Permissions.check(x, "pingspam.addplayershortname", 2))
                            .then(argument("shortname", StringArgumentType.word())
                                .executes(ShortnameCommand::addPlayerShortname)))
                        .then(literal("remove")
                            .requires(x -> Permissions.check(x, "pingspam.removeplayershortname", 2))
                            .then(argument("shortname", StringArgumentType.word())
                                .executes(ShortnameCommand::removePlayerShortname)))))
        );
    }

    private static int removePlayerShortname(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerCommandSource src = ctx.getSource();
        String shortname = StringArgumentType.getString(ctx, "shortname");
        ServerPlayerEntity player = EntityArgumentType.getPlayer(ctx, "player");
        List<String> shortnames = ((ServerPlayerEntityAccess) player).pingspam$getShortnames();
        if (!SHORTNAME_PATTERN.asPredicate().test(shortname))
            throw INVALID_SHORTNAME.create();
        if (!shortnames.contains(shortname))
            throw NO_SUCH_SHORTNAME_OTHER.create(player);
        shortnames.remove(shortname);
        sendShortnameRemovedPacket(src.getMinecraftServer().getPlayerManager(), shortname);
        src.sendFeedback(
            new LiteralText("Removed shortname \"" + shortname + "\" from ")
                .append(player.getDisplayName())
                .append(new LiteralText(".")), true);
        return 0;
    }

    private static int addPlayerShortname(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerCommandSource src = ctx.getSource();
        String newShortname = StringArgumentType.getString(ctx, "shortname");
        ServerPlayerEntity player = EntityArgumentType.getPlayer(ctx, "player");
        List<String> shortnames = ((ServerPlayerEntityAccess) player).pingspam$getShortnames();
        if (!SHORTNAME_PATTERN.asPredicate().test(newShortname))
            throw INVALID_SHORTNAME.create();
        if (shortnames.contains(newShortname))
            throw SHORTNAME_EXISTS_OTHER.create(player);
        if (checkShortnameForCollision(src.getMinecraftServer(), newShortname))
            throw SHORTNAME_COLLISION.create();
        shortnames.add(newShortname);
        sendShortnameAddedPacket(src.getMinecraftServer().getPlayerManager(), newShortname);
        src.sendFeedback(
            new LiteralText("Added shortname \"" + newShortname + "\" to ")
                .append(player.getDisplayName())
                .append(new LiteralText(".")), true);
        return 0;
    }

    private static int listPlayerShortnames(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerCommandSource src = ctx.getSource();
        ServerPlayerEntity player = EntityArgumentType.getPlayer(ctx, "player");
        List<String> shortnames = ((ServerPlayerEntityAccess) player).pingspam$getShortnames();
        StringBuilder sb = new StringBuilder();
        sb.append(" has ");
        sb.append(shortnames.size());
        sb.append(" shortname");
        if (shortnames.size() != 1)
            sb.append('s');
        if (shortnames.size() > 0) {
            sb.append(": ");
            boolean isFirst = true;
            for (String shortname : shortnames) {
                if (!isFirst)
                    sb.append(", ");
                isFirst = false;
                sb.append(shortname);
            }
        } else {
            sb.append('.');
        }
        src.sendFeedback(player.getDisplayName().shallowCopy().append(new LiteralText(sb.toString())), false);

        return 0;
    }

    private static int removeShortname(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerCommandSource src = ctx.getSource();
        String shortname = StringArgumentType.getString(ctx, "shortname");
        ServerPlayerEntity player = src.getPlayer();
        List<String> shortnames = ((ServerPlayerEntityAccess) player).pingspam$getShortnames();
        if (!SHORTNAME_PATTERN.asPredicate().test(shortname))
            throw INVALID_SHORTNAME.create();
        if (!shortnames.contains(shortname))
            throw NO_SUCH_SHORTNAME.create();
        shortnames.remove(shortname);
        sendShortnameRemovedPacket(src.getMinecraftServer().getPlayerManager(), shortname);
        src.sendFeedback(new LiteralText("Removed shortname \"" + shortname + "\"."), false);
        return 0;
    }

    private static int addShortname(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerCommandSource src = ctx.getSource();
        String newShortname = StringArgumentType.getString(ctx, "shortname");
        ServerPlayerEntity player = src.getPlayer();
        List<String> shortnames = ((ServerPlayerEntityAccess) player).pingspam$getShortnames();
        if (!SHORTNAME_PATTERN.asPredicate().test(newShortname))
            throw INVALID_SHORTNAME.create();
        if (shortnames.contains(newShortname))
            throw SHORTNAME_EXISTS.create();
        if (checkShortnameForCollision(src.getMinecraftServer(), newShortname))
            throw SHORTNAME_COLLISION.create();
        shortnames.add(newShortname);
        sendShortnameAddedPacket(src.getMinecraftServer().getPlayerManager(), newShortname);
        src.sendFeedback(new LiteralText("Added shortname \"" + newShortname + "\"."), false);
        return 0;
    }

    private static int listShortnames(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerCommandSource src = ctx.getSource();
        ServerPlayerEntity player = src.getPlayer();
        List<String> shortnames = ((ServerPlayerEntityAccess) player).pingspam$getShortnames();
        StringBuilder sb = new StringBuilder();
        sb.append("You have ");
        sb.append(shortnames.size());
        sb.append(" shortname");
        if (shortnames.size() != 1)
            sb.append('s');
        if (shortnames.size() > 0) {
            sb.append(": ");
            boolean isFirst = true;
            for (String shortname : shortnames) {
                if (!isFirst)
                    sb.append(", ");
                isFirst = false;
                sb.append(shortname);
            }
        } else {
            sb.append('.');
        }
        src.sendFeedback(new LiteralText(sb.toString()), false);

        return 0;
    }

    private static boolean checkShortnameForCollision(MinecraftServer server, String shortname) {
        for (ServerPlayerEntity onlinePlayer : server.getPlayerManager().getPlayerList()) {
            for (String otherShortname : ((ServerPlayerEntityAccess) onlinePlayer).pingspam$getShortnames()) {
                if (otherShortname.equals(shortname))
                    return true;
            }
        }

        for (Map.Entry<UUID, CompoundTag> offlineTag : OfflinePlayerCache.INSTANCE.getPlayers().entrySet()) {
            if (server.getPlayerManager().getPlayer(offlineTag.getKey()) != null)
                continue;
            if (offlineTag.getValue().contains("Shortnames")) {
                ListTag shortnamesTag = offlineTag.getValue().getList("Shortnames", 8);
                for (Tag shortnameTag : shortnamesTag) {
                    if (shortnameTag.asString().equals(shortname))
                        return true;
                }
            }
        }

        return false;
    }

    private static void sendShortnameAddedPacket(PlayerManager manager, String newShortname) {
        PacketByteBuf diffBuf = PacketByteBufs.create();
        diffBuf.writeVarInt(1);
        diffBuf.writeString(newShortname);
        diffBuf.writeVarInt(0);
        manager.sendToAll(ServerPlayNetworking.createS2CPacket(PingSpamPackets.POSSIBLE_NAMES_DIFF, diffBuf));
    }

    private static void sendShortnameRemovedPacket(PlayerManager manager, String shortname) {
        if (PlayerUtils.findPlayer(manager, shortname) == null) {
            PacketByteBuf diffBuf = PacketByteBufs.create();
            diffBuf.writeVarInt(0);
            diffBuf.writeVarInt(1);
            diffBuf.writeString(shortname);
            manager.sendToAll(ServerPlayNetworking.createS2CPacket(PingSpamPackets.POSSIBLE_NAMES_DIFF, diffBuf));
        }
    }
}
