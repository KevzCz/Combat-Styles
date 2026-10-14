package net.pixeldreamstudios.combat_styles.fabric.mixin;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.pixeldreamstudios.combat_styles.kit.KitHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.natamus.starterkit_common_fabric.functions.StarterGearFunctions", remap = false)
public abstract class StarterKitGiveMixin {

    @Unique
    private static final String GIVE_STARTER_KIT =
            "giveStarterKit(Lnet/minecraft/class_1657;Lnet/minecraft/class_2168;Ljava/lang/String;)Ljava/lang/String;";

    @Inject(method = GIVE_STARTER_KIT, at = @At("HEAD"), cancellable = true, require = 0)
    private static void combat_styles$beforeGive(Player player, CommandSourceStack source, String kit,
                                                 CallbackInfoReturnable<String> callback) {
        if (player instanceof ServerPlayer serverPlayer && KitHooks.beforeGive(serverPlayer, kit)) {
            callback.setReturnValue(kit);
        }
    }

    @Inject(method = GIVE_STARTER_KIT, at = @At("RETURN"), require = 0)
    private static void combat_styles$afterGive(Player player, CommandSourceStack source, String kit,
                                                CallbackInfoReturnable<String> callback) {
        if (player instanceof ServerPlayer serverPlayer) {
            KitHooks.afterGive(serverPlayer, callback.getReturnValue());
        }
    }
}
