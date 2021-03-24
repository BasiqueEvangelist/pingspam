package me.basiqueevangelist.pingspam.mixin;

import com.mojang.authlib.GameProfile;
import me.basiqueevangelist.pingspam.PingSpam;
import me.basiqueevangelist.pingspam.access.ServerPlayerEntityAccess;
import me.basiqueevangelist.pingspam.utils.PlayerUtils;
import net.fabricmc.fabric.api.util.NbtType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.*;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin extends PlayerEntity implements ServerPlayerEntityAccess {
    @Unique private static final int ACTIONBAR_TIME = 10;

    public ServerPlayerEntityMixin(World world, BlockPos pos, float yaw, GameProfile profile) {
        super(world, pos, yaw, profile);
    }

    @Shadow public abstract void playSound(SoundEvent event, SoundCategory category, float volume, float pitch);

    @Shadow public ServerPlayNetworkHandler networkHandler;
    @Unique private final List<Text> pings = new ArrayList<>();
    @Unique private final List<String> aliases = new ArrayList<>();
    @Unique private final List<String> pingGroups = new ArrayList<>();
    @Unique private final List<UUID> ignoredPlayers = new ArrayList<>();
    @Unique private SoundEvent pingSound = SoundEvents.BLOCK_BELL_USE;
    @Unique private int actionbarTime = 0;

    @Override
    public List<Text> pingspam$getPings() {
        return pings;
    }

    @Override
    public List<String> pingspam$getAliases() {
        return aliases;
    }

    @Override
    public List<String> pingspam$getPingGroups() {
        return pingGroups;
    }

    @Override
    public List<UUID> pingspam$getIgnoredPlayers() {
        return ignoredPlayers;
    }

    @Override
    public SoundEvent pingspam$getPingSound() {
        return pingSound;
    }

    @Override
    public void pingspam$setPingSound(SoundEvent sound) {
        this.pingSound = sound;
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void tick(CallbackInfo ci) {
        if (pings.size() > 0 && PingSpam.CONFIG.getConfig().showUnreadMessagesInActionbar) {
            actionbarTime--;
            if (actionbarTime <= 0) {
                actionbarTime = ACTIONBAR_TIME;
                networkHandler.sendPacket(new TitleS2CPacket(
                    TitleS2CPacket.Action.ACTIONBAR,
                    new LiteralText("You have " + pings.size() + " unread message" + (pings.size() != 1 ? "s" : "") + ".")
                ));
            }
        } else {
            actionbarTime = 0;
        }
    }

    @Inject(method = "sendMessage(Lnet/minecraft/text/Text;Z)V", at = @At("HEAD"))
    private void onActionbarMessage(Text message, boolean actionBar, CallbackInfo ci) {
        if (actionBar)
            actionbarTime = 70;
    }

    @Inject(method = "readCustomDataFromTag", at = @At("TAIL"))
    private void readDataFromTag(CompoundTag tag, CallbackInfo cb) {
        pings.clear();
        if (tag.contains("UnreadPings")) {
            ListTag pingsTag = tag.getList("UnreadPings", 8);
            for (Tag pingTag : pingsTag) {
                pings.add(Text.Serializer.fromJson(pingTag.asString()));
            }
        }

        aliases.clear();
        if (tag.contains("Shortnames")) {
            ListTag aliasesTag = tag.getList("Shortnames", 8);
            for (Tag aliasTag : aliasesTag) {
                aliases.add(aliasTag.asString());
            }
        }

        pingGroups.clear();
        if (tag.contains("PingGroups")) {
            ListTag pingGroupsTag = tag.getList("PingGroups", 8);
            for (Tag pingGroupTag : pingGroupsTag) {
                pingGroups.add(pingGroupTag.asString());
            }
        }

        ignoredPlayers.clear();
        if (tag.contains("IgnoredPlayers")) {
            ListTag ignoredPlayerListTag = tag.getList("IgnoredPlayers", NbtType.INT_ARRAY);
            for (Tag ignoredPlayerTag : ignoredPlayerListTag) {
                ignoredPlayers.add(NbtHelper.toUuid(ignoredPlayerTag));
            }
        }

        if (tag.contains("PingSound")) {
            if (tag.getString("PingSound").equals("null")) {
                pingSound = null;
            } else {
                pingSound = Registry.SOUND_EVENT.getOrEmpty(new Identifier(tag.getString("PingSound"))).orElse(SoundEvents.BLOCK_BELL_USE);
            }
        }
    }

    @Inject(method = "writeCustomDataToTag", at = @At("TAIL"))
    private void writeDataToTag(CompoundTag tag, CallbackInfo cb) {
        ListTag pingsTag = new ListTag();
        for (Text ping : pings) {
            pingsTag.add(StringTag.of(Text.Serializer.toJson(ping)));
        }
        tag.put("UnreadPings", pingsTag);

        ListTag aliasesTag = new ListTag();
        for (String alias : aliases) {
            aliasesTag.add(StringTag.of(alias));
        }
        tag.put("Shortnames", aliasesTag);

        ListTag pingGroupsTag = new ListTag();
        for (String pingGroup : pingGroups) {
            pingGroupsTag.add(StringTag.of(pingGroup));
        }
        tag.put("PingGroups", pingGroupsTag);

        ListTag ignoredPlayersListTag = new ListTag();
        for (UUID ignoredPlayer : ignoredPlayers) {
            ignoredPlayersListTag.add(NbtHelper.fromUuid(ignoredPlayer));
        }
        tag.put("IgnoredPlayers", ignoredPlayersListTag);

        if (pingSound != null) {
            tag.putString("PingSound", ((SoundEventAccessor) pingSound).pingspam$getId().toString());
        } else {
            tag.putString("PingSound", "null");
        }
    }

    @Inject(method = "copyFrom", at = @At("TAIL"))
    private void copyDataFrom(ServerPlayerEntity oldPlayer, boolean alive, CallbackInfo ci) {
        pings.addAll(PlayerUtils.getUnreadPingsFor(oldPlayer));
        aliases.addAll(PlayerUtils.getAliasesOf(oldPlayer));
        pingGroups.addAll(PlayerUtils.getPingGroupsOf(oldPlayer));
        ignoredPlayers.addAll(PlayerUtils.getIgnoredPlayersOf(oldPlayer));
        pingSound = PlayerUtils.getPingSound(oldPlayer);
    }
}
