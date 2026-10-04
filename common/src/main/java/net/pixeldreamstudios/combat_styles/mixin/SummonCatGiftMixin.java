package net.pixeldreamstudios.combat_styles.mixin;

import net.minecraft.world.entity.animal.Cat;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.pixeldreamstudios.combat_styles.entity.SummonOwner;

@Mixin(targets = "net.minecraft.world.entity.animal.Cat$CatRelaxOnOwnerGoal")
public abstract class SummonCatGiftMixin {

    @Shadow
    @Final
    private Cat cat;

    @Inject(method = "giveMorningGift", at = @At("HEAD"), cancellable = true)
    private void combat_styles$noGift(CallbackInfo callback) {
        if (SummonOwner.isSummon(cat)) {
            callback.cancel();
        }
    }
}
