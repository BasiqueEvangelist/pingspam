package me.basiqueevangelist.pingspam.mixin;

import com.mojang.brigadier.tree.CommandNode;
import me.basiqueevangelist.pechkin.hack.StateTracker;
import net.minecraft.server.command.CommandManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

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

}
