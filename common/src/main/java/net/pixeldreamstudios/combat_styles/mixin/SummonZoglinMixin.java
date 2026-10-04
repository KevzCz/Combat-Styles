package net.pixeldreamstudios.combat_styles.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Zoglin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.pixeldreamstudios.combat_styles.entity.SummonBrains;
import net.pixeldreamstudios.combat_styles.entity.SummonOwner;

import java.util.Optional;

@Mixin(Zoglin.class)
public abstract class SummonZoglinMixin {

    @Inject(method = "findNearestValidAttackTarget", at = @At("HEAD"), cancellable = true)
    private void combat_styles$summonsPickNoTarget(
            CallbackInfoReturnable<Optional<? extends LivingEntity>> callback) {
        Zoglin self = (Zoglin) (Object) this;
        if (SummonOwner.isSummon(self)) {
            callback.setReturnValue(SummonBrains.chosenTarget(self));
        }
    }
}
