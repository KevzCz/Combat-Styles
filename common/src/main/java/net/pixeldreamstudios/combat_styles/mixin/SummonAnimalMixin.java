package net.pixeldreamstudios.combat_styles.mixin;

import net.minecraft.world.entity.animal.Animal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.pixeldreamstudios.combat_styles.entity.SummonOwner;

@Mixin(Animal.class)
public abstract class SummonAnimalMixin {

    @Inject(method = "canFallInLove", at = @At("HEAD"), cancellable = true)
    private void combat_styles$noLove(CallbackInfoReturnable<Boolean> callback) {
        if (SummonOwner.isSummon((Animal) (Object) this)) {
            callback.setReturnValue(false);
        }
    }

    @Inject(method = "canMate", at = @At("HEAD"), cancellable = true)
    private void combat_styles$noMating(Animal partner, CallbackInfoReturnable<Boolean> callback) {
        if (SummonOwner.isSummon((Animal) (Object) this) || SummonOwner.isSummon(partner)) {
            callback.setReturnValue(false);
        }
    }
}
