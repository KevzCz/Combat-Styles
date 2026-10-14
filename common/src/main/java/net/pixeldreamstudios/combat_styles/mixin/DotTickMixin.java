package net.pixeldreamstudios.combat_styles.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import net.pixeldreamstudios.combat_styles.skill.SkillDots;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = {"net.minecraft.world.effect.WitherMobEffect", "net.minecraft.world.effect.PoisonMobEffect"})
public class DotTickMixin {

    @Unique
    private static final float COMBAT_STYLES$MIN_HEALTH = 1.0F;

    @WrapOperation(method = "applyEffectTick",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private boolean combat_styles$scaleDot(LivingEntity victim, DamageSource source, float amount,
                                           Operation<Boolean> original) {
        MobEffect self = (MobEffect) (Object) this;
        float scaled = SkillDots.tick(victim, BuiltInRegistries.MOB_EFFECT.wrapAsHolder(self), amount);
        if (scaled > amount && !source.is(DamageTypes.WITHER)) {
            scaled = Math.min(scaled, Math.max(amount, victim.getHealth() - COMBAT_STYLES$MIN_HEALTH));
        }
        return original.call(victim, source, scaled);
    }
}
