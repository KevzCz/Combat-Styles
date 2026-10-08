package net.pixeldreamstudios.combat_styles.affix;

import net.pixeldreamstudios.combat_styles.damage.DamageHurt;
import net.pixeldreamstudios.combat_styles.damage.DamageSpec;
import net.pixeldreamstudios.combat_styles.damage.DamageDealer;
import net.pixeldreamstudios.combat_styles.platform.SpellPowerAccess;
import java.util.List;
import java.util.Optional;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.BuiltInRegistries;

public final class AffixImpacts {

    static final float MINIMUM_DAMAGE = 0.01F;
    static final int SECOND_IN_TICKS = 20;
    private static final double UNTARGETED_EFFECT_RADIUS = 3.0D;

    private AffixImpacts() {
    }

    private static Optional<DamageSource> splashSourceFor(Player player,
                                                          AffixEffects.Splash splash) {
        if (splash == null) {
            return Optional.empty();
        }
        return splash.damageSpec().flatMap(spec -> DamageDealer.specSourceFor(player, spec));
    }

    private static AffixEffects.Damage attunedTo(Player player, AffixEffects.Damage damage,
                                                 AffixEffects.Scaling scaling) {
        if (!scaling.strongest() || scaling.sources().size() < 2) {
            return damage;
        }
        String winner = SpellPowerAccess.strongestOf(player, scaling.sources());
        if (winner == null || !winner.startsWith(SpellPowerAccess.MOD_ID + ":")) {
            return damage;
        }
        String school = winner.substring(SpellPowerAccess.MOD_ID.length() + 1);
        if (damage.school().filter(school::equals).isPresent()) {
            return damage;
        }
        return new AffixEffects.Damage(damage.mode(), damage.damageType(), Optional.of(school),
                damage.ratio(), damage.flat(), damage.ignoresArmour(), damage.scaling(),
                damage.multiplier());
    }

    static float ratioOf(Player player, AffixEffects.Damage damage) {
        float ratio = Math.max(0.0F, damage.ratio());
        if (damage.scaling().isEmpty()) {
            return ratio;
        }
        AffixEffects.Scaling scaling = damage.scaling().get();
        return scaling.apply(ratio,
                SpellPowerAccess.valueOf(player, scaling.sources(), scaling.strongest()));
    }

    static float convertedShare(Player player, StyleAffix converter) {
        if (converter == null) {
            return 0.0F;
        }
        float share = 0.0F;
        for (AffixEffects.Damage damage : converter.onHit().allDamage()) {
            if (damage.converts()) {
                share += ratioOf(player, damage);
            }
        }
        return Math.min(1.0F, share);
    }

    static void dealPhysical(Player player, LivingEntity target, float amount) {
        if (amount >= MINIMUM_DAMAGE) {
            DamageHurt.bypassingIFrames(target, player.damageSources().playerAttack(player), amount);
        }
    }

    static float dealDamage(Player player, LivingEntity target,
                                    AffixEffects.Damage damage, float dealtDamage) {
        float ratio = ratioOf(player, damage);
        if (damage.scaling().isPresent()) {
            damage = attunedTo(player, damage, damage.scaling().get());
        }
        float amount = (dealtDamage * ratio + Math.max(0.0F, damage.flat()))
                * Math.max(0.0F, damage.multiplier());
        if (amount < MINIMUM_DAMAGE) {
            return 0.0F;
        }

        DamageSource source = sourceFor(player, damage);
        if (source == null) {
            return 0.0F;
        }

        DamageHurt.bypassingIFrames(target, source, amount);
        return amount;
    }

    static void applyAreaEffects(Player player, StyleAffix affix,
                                         AffixEffects.OnHit onHit, float base) {
        AffixEffects.Splash splash = onHit.splash().orElse(null);
        boolean secondary = onHit.ignite().isPresent() || onHit.knockback().isPresent()
                || onHit.freezeTicks() > 0.0F || !onHit.statusEffects().isEmpty();
        if (splash == null && !secondary) {
            return;
        }
        double radius = splash == null ? UNTARGETED_EFFECT_RADIUS : splash.radius();
        boolean hitsAttacker = splash != null && splash.hitsAttacker();
        AABB area = player.getBoundingBox().inflate(radius);
        List<LivingEntity> nearby = player.level().getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity.isAlive()
                        && (hitsAttacker ? entity != player : AffixTargets.canHit(player, entity)));
        if (nearby.isEmpty()) {
            return;
        }

        float amount = splash == null
                ? 0.0F
                : DamageSpec.resolve(splash.damageSpec(), player, base, splash.ratio());
        DamageSource source = splashSourceFor(player, splash)
                .or(() -> typedDamageOf(onHit).map(damage -> sourceFor(player, damage)))
                .orElseGet(() -> player.damageSources().playerAttack(player));

        for (LivingEntity entity : nearby) {
            if (source != null && amount >= MINIMUM_DAMAGE) {
                DamageDealer.dealSpec(player, entity,
                        splash == null ? null : splash.damageSpec().orElse(null),
                        amount, source);
            }
            onHit.ignite().ifPresent(ignite -> {
                if (EffectChance.rolls(player, ignite.chance())) {
                    entity.setRemainingFireTicks(Math.max(entity.getRemainingFireTicks(),
                            ignite.seconds() * SECOND_IN_TICKS));
                }
            });
            if (onHit.freezeTicks() > 0.0F) {
                entity.setTicksFrozen((int) Math.max(entity.getTicksFrozen(),
                        onHit.freezeTicks()));
            }
            for (AffixEffects.StatusEffect effect : onHit.statusEffects()) {
                if (!EffectChance.rolls(player, effect.chance())) {
                    continue;
                }
                mobEffect(effect.effect()).ifPresent(holder -> entity.addEffect(
                        new MobEffectInstance(holder, effect.duration(), effect.amplifier())));
            }
            onHit.knockback().ifPresent(knockback -> {
                Vec3 direction = entity.position().subtract(player.position()).normalize();
                if (knockback.pull()) {
                    direction = direction.reverse();
                }
                entity.push(direction.x * knockback.strength(), 0.1D,
                        direction.z * knockback.strength());
                entity.hurtMarked = true;
            });
        }
    }

    static void applySplash(Player player, LivingEntity origin, StyleAffix affix,
                                    AffixEffects.Splash splash, float total,
                                    DamageSource source) {
        float amount = DamageSpec.resolve(splash.damageSpec(), player, total, splash.ratio());
        if (amount < MINIMUM_DAMAGE) {
            return;
        }
        AABB area = origin.getBoundingBox().inflate(splash.radius());
        DamageSource typed = splashSourceFor(player, splash).orElse(source);
        if (typed == null) {
            return;
        }
        source = typed;
        List<LivingEntity> nearby = player.level().getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity != origin
                        && entity.isAlive()
                        && (splash.hitsAttacker()
                                ? entity != player
                                : AffixTargets.canHit(player, entity)));
        for (LivingEntity entity : nearby) {
            DamageDealer.dealSpec(player, entity, splash.damageSpec().orElse(null), amount, source);
        }
    }

    static void spawnParticles(ServerLevel level, LivingEntity target,
                                       AffixEffects.Particles particles) {
        ResourceLocation particleId = ResourceLocation.tryParse(particles.type());
        if (particleId == null) {
            return;
        }
        ParticleType<?> type = BuiltInRegistries.PARTICLE_TYPE.get(particleId);
        if (!(type instanceof SimpleParticleType simple)) {
            return;
        }
        level.sendParticles((ParticleOptions) simple,
                target.getX(), target.getY(0.5D), target.getZ(),
                particles.count(),
                particles.spread(), particles.spread(), particles.spread(),
                particles.speed());
    }

    public static void applySelfEffects(Player player,
                                        List<AffixEffects.SelfEffect> effects) {
        if (player == null || effects == null || effects.isEmpty()) {
            return;
        }
        for (AffixEffects.SelfEffect effect : effects) {
            mobEffect(effect.effect()).ifPresent(holder -> AffixMechanics.applyStatus(
                    player, holder, effect.duration(), effect.amplifier(),
                    effect.stacks(), effect.maxAmplifier()));
        }
    }

    static Optional<Holder<MobEffect>> mobEffect(String effectId) {
        ResourceLocation id = ResourceLocation.tryParse(effectId);
        if (id == null) {
            return Optional.empty();
        }
        return BuiltInRegistries.MOB_EFFECT.getHolder(ResourceKey.create(Registries.MOB_EFFECT, id))
                .map(holder -> holder);
    }

    private static Optional<AffixEffects.Damage> typedDamageOf(AffixEffects.OnHit onHit) {
        return onHit.allDamage().stream()
                .filter(damage -> !damage.amplifies())
                .findFirst();
    }

    static DamageSource spreadSourceFor(Player player, AffixEffects.OnHit onHit,
                                                AffixEffects.Typed spread) {
        if (spread != null) {
            String declared = spread.damageType().orElse(null);
            if (declared != null && !declared.isEmpty()) {
                return DamageDealer.typedSource(player, declared);
            }
            String school = spread.school().orElse(null);
            if (school != null && !school.isEmpty()) {
                return DamageDealer.typedSource(player, SpellPowerAccess.damageTypeOf(school));
            }
            if (!spread.inherit()) {
                return player.damageSources().playerAttack(player);
            }
        }
        return typedDamageOf(onHit)
                .map(damage -> sourceFor(player, damage))
                .orElseGet(() -> player.damageSources().playerAttack(player));
    }

    private static DamageSource sourceFor(Player player, AffixEffects.Damage damage) {
        String declared = damage.damageType();
        ResourceLocation typeId = declared == null || declared.isEmpty()
                ? null
                : ResourceLocation.tryParse(declared);
        if (typeId == null) {
            typeId = damage.school().map(SpellPowerAccess::damageTypeOf).orElse(null);
        }
        return DamageDealer.typedSource(player, typeId);
    }
}
