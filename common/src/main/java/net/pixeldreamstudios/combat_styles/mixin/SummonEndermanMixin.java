package net.pixeldreamstudios.combat_styles.mixin;

import net.minecraft.world.entity.monster.EnderMan;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.pixeldreamstudios.combat_styles.entity.SummonOwner;

@Mixin(targets = "net.minecraft.world.entity.monster.EnderMan$EndermanTakeBlockGoal")
public abstract class SummonEndermanMixin {

    @Shadow
    @Final
    private EnderMan enderman;

    @Inject(method = "canUse", at = @At("HEAD"), cancellable = true)
    private void combat_styles$noBlockTaking(CallbackInfoReturnable<Boolean> callback) {
        if (SummonOwner.isSummon(enderman)) {
            callback.setReturnValue(false);
        }
    }
}
