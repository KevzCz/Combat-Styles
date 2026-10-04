package net.pixeldreamstudios.combat_styles.mixin;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.pixeldreamstudios.combat_styles.entity.SummonOwner;

@Mixin(targets = "net.minecraft.world.entity.monster.Silverfish$SilverfishMergeWithStoneGoal")
public abstract class SummonSilverfishMixin extends RandomStrollGoal {

    private SummonSilverfishMixin(PathfinderMob mob, double speed) {
        super(mob, speed);
    }

    @Inject(method = "canUse", at = @At("HEAD"), cancellable = true)
    private void combat_styles$noMerging(CallbackInfoReturnable<Boolean> callback) {
        if (SummonOwner.isSummon(mob)) {
            callback.setReturnValue(false);
        }
    }
}
