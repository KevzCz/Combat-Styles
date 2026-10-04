package net.pixeldreamstudios.combat_styles.mixin;

import net.minecraft.world.entity.animal.goat.Goat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.pixeldreamstudios.combat_styles.entity.SummonOwner;

@Mixin(Goat.class)
public abstract class SummonGoatMixin {

    @Inject(method = "dropHorn", at = @At("HEAD"), cancellable = true)
    private void combat_styles$noHorn(CallbackInfoReturnable<Boolean> callback) {
        if (SummonOwner.isSummon((Goat) (Object) this)) {
            callback.setReturnValue(false);
        }
    }
}
