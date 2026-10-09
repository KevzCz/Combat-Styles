package net.pixeldreamstudios.combat_styles.damage;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import net.pixeldreamstudios.combat_styles.mixin.LivingEntityHurtAccessor;

public final class DamageHurt {

    private static final int IFRAME_THRESHOLD = 10;

    private DamageHurt() {
    }

    public static boolean isThrottled(Entity entity) {
        return entity instanceof LivingEntity living && living.invulnerableTime > IFRAME_THRESHOLD;
    }

    public static void clearIFrames(LivingEntity target) {
        target.invulnerableTime = 0;
        ((LivingEntityHurtAccessor) target).combat_styles$setLastHurt(0.0F);
    }

    public static boolean bypassingIFrames(LivingEntity target, DamageSource source, float amount) {
        LivingEntityHurtAccessor accessor = (LivingEntityHurtAccessor) target;
        int previousInvulnerable = target.invulnerableTime;
        float previousLastHurt = accessor.combat_styles$getLastHurt();

        target.invulnerableTime = 0;
        accessor.combat_styles$setLastHurt(0.0F);
        try {
            return target.hurt(FiringItem.stated(source), amount);
        } finally {
            target.invulnerableTime = Math.max(previousInvulnerable, target.invulnerableTime);
            accessor.combat_styles$setLastHurt(
                    Math.max(previousLastHurt, accessor.combat_styles$getLastHurt()));
        }
    }
}
