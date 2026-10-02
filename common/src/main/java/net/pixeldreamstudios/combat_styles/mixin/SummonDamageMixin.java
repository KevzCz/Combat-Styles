package net.pixeldreamstudios.combat_styles.mixin;

import net.pixeldreamstudios.combat_styles.damage.DamageDealer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.pixeldreamstudios.combat_styles.damage.DamageHurt;
import net.pixeldreamstudios.combat_styles.entity.SummonStrike;

@Mixin(LivingEntity.class)
public abstract class SummonDamageMixin {

    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true, require = 1)
    private void combat_styles$summonDamage(DamageSource source, float amount,
                                            CallbackInfoReturnable<Boolean> callback) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level().isClientSide() || DamageDealer.isDealing() || amount <= 0.0F
                || self.isInvulnerableTo(source) || DamageHurt.isThrottled(self)) {
            return;
        }
        if (SummonStrike.shieldsOwner(self, source)) {
            callback.setReturnValue(false);
            return;
        }
        if (SummonStrike.deal(self, source, amount)) {
            callback.setReturnValue(true);
        }
    }
}
