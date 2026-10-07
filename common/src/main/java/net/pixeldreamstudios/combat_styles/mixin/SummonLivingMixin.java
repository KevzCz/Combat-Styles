package net.pixeldreamstudios.combat_styles.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.pixeldreamstudios.combat_styles.entity.SummonLifecycle;
import net.pixeldreamstudios.combat_styles.entity.SummonOwner;

@Mixin(LivingEntity.class)
public abstract class SummonLivingMixin {

    @Inject(method = "dropAllDeathLoot", at = @At("HEAD"), cancellable = true)
    private void combat_styles$noDeathLoot(ServerLevel level, DamageSource source,
                                           CallbackInfo callback) {
        if (SummonOwner.isSummon((LivingEntity) (Object) this)) {
            callback.cancel();
        }
    }

    @Inject(method = "checkTotemDeathProtection", at = @At("HEAD"), cancellable = true)
    private void combat_styles$noRevival(DamageSource source, CallbackInfoReturnable<Boolean> callback) {
        if (SummonOwner.isSummon((LivingEntity) (Object) this)) {
            callback.setReturnValue(false);
        }
    }

    @Inject(method = "die", at = @At("HEAD"))
    private void combat_styles$summonDeath(DamageSource source, CallbackInfo callback) {
        SummonLifecycle.died((LivingEntity) (Object) this, source);
    }

    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void combat_styles$noFriendlyDamage(DamageSource source, float amount,
                                                CallbackInfoReturnable<Boolean> callback) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (SummonOwner.friendly(source.getEntity(), self)
                || SummonOwner.friendly(source.getDirectEntity(), self)) {
            callback.setReturnValue(false);
        }
    }

    @Inject(method = "hurt", at = @At("RETURN"))
    private void combat_styles$summonHurt(DamageSource source, float amount,
                                          CallbackInfoReturnable<Boolean> callback) {
        if (callback.getReturnValueZ()) {
            SummonLifecycle.hurt((LivingEntity) (Object) this, source);
        }
    }

    @Inject(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",
            at = @At("HEAD"), cancellable = true)
    private void combat_styles$noFriendlyDebuff(MobEffectInstance effect, Entity source,
                                                CallbackInfoReturnable<Boolean> callback) {
        if (effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL
                && SummonOwner.friendly(source, (LivingEntity) (Object) this)) {
            callback.setReturnValue(false);
        }
    }
}
