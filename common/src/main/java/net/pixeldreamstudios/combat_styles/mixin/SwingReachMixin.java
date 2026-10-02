package net.pixeldreamstudios.combat_styles.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.bettercombat.network.ServerNetwork;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.pixeldreamstudios.combat_styles.equipment.SwingDrag;

@Mixin(value = ServerNetwork.class, priority = 1500)
public class SwingReachMixin {

    @ModifyExpressionValue(
            method = "handleAttackRequest",
            at = @At(value = "FIELD",
                    target = "Lnet/bettercombat/network/Packets$C2S_AttackRequest;"
                            + "UseVanillaPacket:Z"),
            remap = false,
            require = 1)
    private static boolean combat_styles$bypassVanillaPacket(boolean useVanillaPacket) {
        return useVanillaPacket && !SwingDrag.active();
    }

    @ModifyExpressionValue(
            method = "lambda$handleAttackRequest$3",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerPlayer;"
                            + "distanceToSqr(Lnet/minecraft/world/entity/Entity;)D"),
            remap = false,
            require = 1)
    private static double combat_styles$shortenDraggedDistance(double distance) {
        return SwingDrag.validatedDistance(distance);
    }

    @Inject(method = "lambda$handleAttackRequest$3", at = @At("RETURN"),
            remap = false, require = 1)
    private static void combat_styles$clearDrag(CallbackInfo callback) {
        SwingDrag.end();
    }
}
