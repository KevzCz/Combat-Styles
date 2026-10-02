package net.pixeldreamstudios.combat_styles.affix;

import net.pixeldreamstudios.combat_styles.compat.SpellEngineCompat;
import net.pixeldreamstudios.combat_styles.damage.DamageSpec;
import net.pixeldreamstudios.combat_styles.CombatStyles;
import net.pixeldreamstudios.combat_styles.mixin.ArrowPickupAccessor;
import net.pixeldreamstudios.combat_styles.debug.CombatDebug;
import net.pixeldreamstudios.combat_styles.entity.SummonData;
import net.pixeldreamstudios.combat_styles.entity.SlashWaveEntity;
import net.pixeldreamstudios.combat_styles.equipment.SpellTuning;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.core.registries.BuiltInRegistries;

final class AffixProjectiles {

    private static final String PROJECTILE_DEBUG = "projectile";

    private AffixProjectiles() {
    }

    private static void shootSpellProjectiles(Player player, LivingEntity target,
                                              AffixEffects.Projectile projectile,
                                              float triggeringDamage) {
        if (!SpellEngineCompat.isLoaded()) {
            CombatDebug.note(player, "spell projectile skipped — Spell Engine not installed");
            return;
        }
        ResourceLocation spellId =
                ResourceLocation.tryParse(projectile.spellProjectile().spell());
        if (spellId == null) {
            return;
        }
        float damage = spellDamageOf(player, projectile, triggeringDamage);
        int count = Math.max(1, projectile.count());
        DamageSpec spec = projectile.damageSpec().orElse(null);
        SpellTuning tuning = projectile.spellProjectile().tuning();
        for (int shot = 0; shot < count; shot++) {
            Object tag = SpellEngineCompat.shootSpell(player, spellId, target, damage, tuning);
            if (tag == null) {
                if (CombatDebug.enabled(player)) {
                    CombatDebug.note(player, "spell " + spellId
                            + " did not launch — it needs a projectile or meteor delivery");
                }
                return;
            }
            ProjectileUpkeep.watchSpellShot(tag, player, spec, projectile.ignoreIframes());
        }
    }

    private static float spellDamageOf(Player player, AffixEffects.Projectile projectile,
                                       float triggeringDamage) {
        if (projectile.damageSpec().isEmpty() && projectile.damage() <= 0.0F) {
            return SpellEngineCompat.INHERIT_DAMAGE;
        }
        return projectileDamage(player, projectile, triggeringDamage);
    }

    static void spawn(ServerLevel level, Player player, LivingEntity target,
                                         AffixEffects.Projectile projectile,
                                         float triggeringDamage) {
        if (!EffectChance.rolls(player, projectile.chance())) {
            return;
        }
        if (!projectile.spellProjectile().isEmpty()) {
            shootSpellProjectiles(player, target, projectile, triggeringDamage);
            return;
        }
        if (projectile.entity().isBlank()) {
            CombatDebug.note(player, "projectile has no entity and no spell_projectile");
            return;
        }
        ResourceLocation entityId = ResourceLocation.tryParse(projectile.entity());
        if (entityId == null) {
            return;
        }
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(entityId).orElse(null);
        if (type == null) {
            CombatStyles.LOGGER.warn("Affix projectile entity {} does not exist", entityId);
            return;
        }

        ProjectileSpawn spawn = projectile.spawn();
        boolean fromTarget = spawn.anchor().usesTarget() && target != null;
        Vec3 baseDirection = fromTarget
                ? target.position().subtract(player.position()).normalize()
                : player.getLookAngle();

        float hitDamage = projectileDamage(player, projectile, triggeringDamage);
        int count = projectile.count();
        Vec3 carried = spawn.carriedMotion(player);

        for (int i = 0; i < count; i++) {
            Entity spawned = type.create(level);
            if (spawned == null) {
                return;
            }
            Vec3 origin = spawn.originFor(player, target, i, count);
            Vec3 aimed = spawn.directionFor(player, target, baseDirection, i, count);
            Vec3 direction = spawn.mode().scattersDirection()
                    ? jitter(aimed, projectile.spread(), player)
                    : aimed;
            spawned.setPos(origin.x, origin.y, origin.z);
            if (spawned instanceof Projectile shot) {
                shot.setOwner(player);
                shot.shoot(direction.x, direction.y, direction.z, projectile.speed(),
                        spawn.velocity().divergence());
                if (carried.lengthSqr() > 0.0D) {
                    shot.setDeltaMovement(shot.getDeltaMovement().add(carried));
                }
            } else {
                if (spawned instanceof SlashWaveEntity wave) {
                    wave.setOwner(player);
                }
                spawned.setDeltaMovement(direction.scale(projectile.speed()).add(carried));
            }
            if (spawn.velocity().overridesGravity()) {
                spawned.setNoGravity(true);
            }
            applyProjectileDamage(player, spawned, projectile, hitDamage);
            if (spawned instanceof AbstractArrow arrow) {
                arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
                if (arrow.getPickupItemStackOrigin().isEmpty()) {
                    ((ArrowPickupAccessor) arrow)
                            .combat_styles$setPickupItemStack(new ItemStack(Items.ARROW));
                }
            }
            ProjectileUpkeep.track(player, spawned, projectile);
            level.addFreshEntity(spawned);
        }
    }

    private static float projectileDamage(Player player, AffixEffects.Projectile projectile,
                                          float triggeringDamage) {
        if (projectile.damageSpec().isPresent()) {
            return projectile.damageSpec().get().amountFor(player, triggeringDamage);
        }
        return Math.max(0.0F, projectile.damage());
    }

    private static void applyProjectileDamage(Player player, Entity spawned,
                                              AffixEffects.Projectile projectile,
                                              float hitDamage) {
        if (hitDamage <= 0.0F) {
            return;
        }
        if (spawned instanceof AbstractArrow arrow) {
            arrow.setBaseDamage(hitDamage);
            return;
        }
        if (spawned instanceof SummonData data) {
            data.combat_styles$setSummonOwner(player);
            data.combat_styles$setSummonDamage(hitDamage);
            data.combat_styles$setSummonSpec(projectile.damageSpec().orElse(null));
            if (CombatDebug.enabled(player)) {
                CombatDebug.fired(player, PROJECTILE_DEBUG,
                        projectile.entity() + " carries " + hitDamage + " via summon data");
            }
            return;
        }
        if (CombatDebug.enabled(player)) {
            CombatDebug.skipped(player, PROJECTILE_DEBUG,
                    projectile.entity() + " cannot carry a damage value");
        }
    }

    private static Vec3 jitter(Vec3 direction, float spread, Player player) {
        if (spread <= 0.0F) {
            return direction;
        }
        return direction.add(
                (player.getRandom().nextDouble() - 0.5D) * spread,
                (player.getRandom().nextDouble() - 0.5D) * spread,
                (player.getRandom().nextDouble() - 0.5D) * spread).normalize();
    }
}
