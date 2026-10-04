package net.pixeldreamstudios.combat_styles.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.hoglin.HoglinAi;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.pixeldreamstudios.combat_styles.entity.SummonBrains;
import net.pixeldreamstudios.combat_styles.entity.SummonOwner;

import java.util.Optional;

@Mixin(HoglinAi.class)
public abstract class SummonHoglinAiMixin {

    @Inject(method = "findNearestValidAttackTarget", at = @At("HEAD"), cancellable = true)
    private static void combat_styles$summonsPickNoTarget(Hoglin hoglin,
            CallbackInfoReturnable<Optional<? extends LivingEntity>> callback) {
        if (SummonOwner.isSummon(hoglin)) {
            callback.setReturnValue(SummonBrains.chosenTarget(hoglin));
        }
    }
}
