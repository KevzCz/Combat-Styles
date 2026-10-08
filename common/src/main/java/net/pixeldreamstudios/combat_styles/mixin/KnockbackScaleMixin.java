package net.pixeldreamstudios.combat_styles.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import net.pixeldreamstudios.combat_styles.attribute.MeleeKnockback;

@Mixin(LivingEntity.class)
public abstract class KnockbackScaleMixin {

    @ModifyVariable(method = "knockback", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private double combat_styles$scaleMeleeKnockback(double strength) {
        return MeleeKnockback.scaled(strength);
    }
}
