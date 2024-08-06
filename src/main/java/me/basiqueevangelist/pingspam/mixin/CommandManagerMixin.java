package me.basiqueevangelist.pingspam.mixin;

import com.mojang.brigadier.tree.CommandNode;
import me.basiqueevangelist.pingspam.hack.StateTracker;
import me.basiqueevangelist.pingspam.network.ServerNetworkLogic;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CommandManager.class)
public class CommandManagerMixin {
    @Redirect(method = "makeTreeForSource", at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/tree/CommandNode;canUse(Ljava/lang/Object;)Z"))
    private boolean canUse(CommandNode<Object> node, Object source) {
        StateTracker.IS_IN_COMMAND_TREE_CREATION = true;
        try {
            return node.canUse(source);
        } finally {
            StateTracker.IS_IN_COMMAND_TREE_CREATION = false;
        }
    }

    @Inject(method = "sendCommandTree(Lnet/minecraft/server/network/ServerPlayerEntity;)V", at = @At("HEAD"))
    private void onReloadPermissions(ServerPlayerEntity player, CallbackInfo ci) {
        ServerNetworkLogic.sendServerAnnouncement(player, player.networkHandler.connection);
    }
}
