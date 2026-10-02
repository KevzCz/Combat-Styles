package net.pixeldreamstudios.combat_styles.equipment;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public record ProjectileHitEvent(Entity projectile, LivingEntity victim, DamageSource source,
                                 float amount, double distance, boolean viaSummon) {
}
