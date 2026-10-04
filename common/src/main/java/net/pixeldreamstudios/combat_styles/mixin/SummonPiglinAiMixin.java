package net.pixeldreamstudios.combat_styles.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.pixeldreamstudios.combat_styles.entity.SummonBrains;
import net.pixeldreamstudios.combat_styles.entity.SummonOwner;

import java.util.Optional;

@Mixin(PiglinAi.class)
public abstract class SummonPiglinAiMixin {

    @Inject(method = "findNearestValidAttackTarget", at = @At("HEAD"), cancellable = true)
    private static void combat_styles$summonsPickNoTarget(Piglin piglin,
            CallbackInfoReturnable<Optional<? extends LivingEntity>> callback) {
        if (SummonOwner.isSummon(piglin)) {
            callback.setReturnValue(SummonBrains.chosenTarget(piglin));
        }
    }
}
