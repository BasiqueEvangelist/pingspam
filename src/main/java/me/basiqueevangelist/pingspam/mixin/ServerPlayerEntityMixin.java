package me.basiqueevangelist.pingspam.mixin;

import me.basiqueevangelist.pingspam.access.ServerPlayerEntityAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin implements ServerPlayerEntityAccess {
    @Shadow public abstract void playSound(SoundEvent event, SoundCategory category, float volume, float pitch);

    @Shadow public ServerPlayNetworkHandler networkHandler;
    @Unique private final List<Text> pings = new ArrayList<>();
    @Unique private int actionbarTime = 0;

    @Override
    public List<Text> pingspam$getPings() {
        return pings;
    }

    @Override
    public void pingspam$ping(GameMessageS2CPacket msgPacket) {
        GameMessageS2CPacketAccessor access = (GameMessageS2CPacketAccessor) msgPacket;

        playSound(SoundEvents.BLOCK_BELL_USE, SoundCategory.PLAYERS, 1.5F, 1.0F);
        pings.add(access.pingspam$getMessage());

        Text pingMessage = access.pingspam$getMessage().shallowCopy().formatted(Formatting.BLUE);
        networkHandler.sendPacket(new GameMessageS2CPacket(pingMessage, access.pingspam$getLocation(), access.pingspam$getSenderUuid()));
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void tick(CallbackInfo ci) {
        if (pings.size() > 0) {
            actionbarTime++;
            if (actionbarTime >= 5) {
                actionbarTime = 0;
                networkHandler.sendPacket(new TitleS2CPacket(
                        TitleS2CPacket.Action.ACTIONBAR,
                        new LiteralText("You have " + pings.size() + " unread message" + (pings.size() != 1 ? "s" : "") + ".")
                ));
            }
        } else {
            actionbarTime = 5;
        }
    }

    @Inject(method = "readCustomDataFromTag", at = @At("TAIL"))
    private void readPingsFromTag(CompoundTag tag, CallbackInfo cb) {
        pings.clear();
        if (tag.contains("UnreadPings")) {
            ListTag pingsTag = tag.getList("UnreadPings", 8);
            for (Tag pingTag : pingsTag) {
                pings.add(Text.Serializer.fromJson(pingTag.asString()));
            }
        }
    }

    @Inject(method = "writeCustomDataToTag", at = @At("TAIL"))
    private void writePingsFromTag(CompoundTag tag, CallbackInfo cb) {
        ListTag pingsTag = new ListTag();
        for (Text ping : pings) {
            pingsTag.add(StringTag.of(Text.Serializer.toJson(ping)));
        }
        tag.put("UnreadPings", pingsTag);
    }
}
