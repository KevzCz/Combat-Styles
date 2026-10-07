package net.pixeldreamstudios.combat_styles.entity;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import net.pixeldreamstudios.combat_styles.CombatStyles;
import net.pixeldreamstudios.combat_styles.affix.AffixEffects;
import net.pixeldreamstudios.combat_styles.affix.EffectChance;
import net.pixeldreamstudios.combat_styles.affix.ProjectileSpawn;
import net.pixeldreamstudios.combat_styles.compat.SpellEngineCompat;
import net.pixeldreamstudios.combat_styles.damage.DamageSpec;
import net.pixeldreamstudios.combat_styles.debug.CombatDebug;
import net.pixeldreamstudios.combat_styles.equipment.SummonProfile;
import net.pixeldreamstudios.combat_styles.equipment.SummonProfiles;
import net.pixeldreamstudios.combat_styles.platform.SpellPowerAccess;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

public final class SummonSpawner {

    private static final int MAX_SUMMONS_PER_EFFECT = 16;
    private static final double SPAWN_MIN_RADIUS = 1.0D;
    private static final double ANGLE_JITTER = 0.6D;
    private static final double REUSE_SEARCH_RADIUS = 24.0D;
    private static final double COMPANION_RADIUS = 2.0D;
    private static final double FULL_TURN = Math.PI * 2.0D;
    private static final String DEBUG_KEY = "summon";
    private static final int RGB_MASK = 0xFFFFFF;
    private static final int RED_SHIFT = 16;
    private static final int GREEN_SHIFT = 8;
    private static final int CHANNEL = 0xFF;
    private static final float CHANNEL_MAX = 255.0F;
    private static final float PARTICLE_SCALE = 1.2F;
    private static final int PARTICLE_COUNT = 16;
    private static final double PARTICLE_SPEED = 0.02D;
    private static final double HALF = 0.5D;
    private static final String KEY_SUMMON_NAME = "entity.combat_styles.summon_name";
    private static final String COMMON_NAMESPACE = "c";
    private static final String BOSSES_TAG = "bosses";
    private static final TagKey<EntityType<?>> BOSSES = TagKey.create(Registries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(COMMON_NAMESPACE, BOSSES_TAG));
    private static final Set<EntityType<?>> NEVER_RAISED = Set.of(EntityType.PLAYER,
            EntityType.ENDER_DRAGON, EntityType.WITHER, EntityType.WARDEN, EntityType.ELDER_GUARDIAN);

    private record Despawn(Entity entity, long expiresAt) {
    }

    private static final List<Despawn> DESPAWNING = new CopyOnWriteArrayList<>();

    private SummonSpawner() {
    }

    public static void tickDespawns(long gameTime) {
        List<Despawn> delayed = new ArrayList<>();
        DESPAWNING.removeIf(despawn -> {
            if (gameTime < despawn.expiresAt()) {
                return false;
            }
            Entity entity = despawn.entity();
            if (!entity.isAlive()) {
                AbilityHosts.forget(entity);
                return true;
            }
            if (entity instanceof ConfigurableSummon summon && summon.beginDespawn()) {
                return summon.fading();
            }
            int finale = AbilityHosts.beginFinale(entity);
            if (finale > 0) {
                delayed.add(new Despawn(entity, gameTime + finale));
                return true;
            }
            AbilityHosts.forget(entity);
            entity.discard();
            return true;
        });
        DESPAWNING.addAll(delayed);
    }

    public static void applySummons(ServerLevel level, Player player, LivingEntity target,
                                    List<AffixEffects.Summon> summons, float dealtDamage) {
        applySummons(level, player, target, summons, dealtDamage, null);
    }

    public static void applySummons(ServerLevel level, Player player, LivingEntity target,
                                    List<AffixEffects.Summon> summons, float dealtDamage,
                                    RaiseRules raise) {
        for (AffixEffects.Summon declared : summons) {
            AffixEffects.Summon summon = resolveVictim(declared, target);
            if (summon == null) {
                continue;
            }
            EntityType<?> type = typeOf(summon.entity());
            if (type == null) {
                CombatStyles.LOGGER.warn("Unknown summon entity {}", summon.entity());
                continue;
            }
            SummonProfile replacing = declared.raisesVictim()
                    ? null
                    : SummonProfiles.replacementFor(player, summon.entity());
            if (replacing != null) {
                EntityType<?> replaced = typeOf(replacing.entity().orElse(""));
                if (replaced != null) {
                    type = replaced;
                }
            }
            RaiseRules raised = raise == null ? RaiseRules.DEFAULT : raise;
            SummonProfile profile = replacing != null
                    ? replacing
                    : declared.raisesVictim()
                    ? raised.profile().profileFor(player, summon.entity(), raised.profiles())
                    : profileFor(player, summon.entity());
            if (!EffectChance.rolls(player, summon.chance())) {
                continue;
            }
            DamageSpec spec = summon.damageSpec()
                    .or(() -> profile == null ? Optional.empty() : profile.damageSpec())
                    .orElse(null);
            float summonDamage = summonDamage(player, summon, spec, dealtDamage,
                    profile == null ? null : profile.damageScaling().orElse(null));
            if (summonDamage <= 0.0F && CombatDebug.enabled(player)) {
                CombatDebug.skipped(player, DEBUG_KEY, summon.entity()
                        + " resolved 0 damage (hit dealt " + dealtDamage + ")");
            }
            if (retargetExisting(level, player, target, summon, summonDamage)) {
                continue;
            }
            spawnBatch(level, player, target, declared, summon, type, profile, spec, summonDamage);
        }
    }

    private static void spawnBatch(ServerLevel level, Player player, LivingEntity target,
                                   AffixEffects.Summon declared, AffixEffects.Summon summon,
                                   EntityType<?> type, SummonProfile profile, DamageSpec spec,
                                   float summonDamage) {
        String source = idOf(type);
        int wanted = Math.min(summon.count(), MAX_SUMMONS_PER_EFFECT);
        int spread = wanted > 1 ? wanted : 0;
        double baseAngle = level.random.nextDouble() * FULL_TURN;
        for (int i = 0; i < wanted; i++) {
            Entity spawned = type.create(level);
            if (spawned == null) {
                break;
            }
            ConfigurableSummon configurable = spawned instanceof ConfigurableSummon summoned
                    ? summoned
                    : null;
            if (configurable != null && !configurable.canSpawn(player, i)) {
                break;
            }
            summon.maxActive().ifPresent(max -> SummonCaps.makeRoom(player, source, max));
            if (declared.raisesVictim() && target != null) {
                spawned.moveTo(target.getX(), target.getY(), target.getZ(),
                        target.getYRot(), target.getXRot());
            } else {
                place(level, player, spawned, configurable, summon, target, i, spread, baseAngle);
            }
            if (configurable != null) {
                configurable.onSpawned(player, i);
                configurable.bindProfile(SummonProfiles.keyOf(player, profile), i);
                configure(player, target, configurable, summon, profile, summonDamage, i);
            } else if (spawned instanceof SummonData data
                    && !summon.ownDamage() && (profile == null || !profile.ownDamage())) {
                data.combat_styles$setSummonDamage(summonDamage);
                data.combat_styles$setSummonSpec(spec);
            }
            if (configurable == null && spawned instanceof LivingEntity hosted
                    && AbilityHosts.wants(profile)) {
                AbilityHosts.track(player, hosted, SummonProfiles.keyOf(player, profile), i,
                        summonDamage);
            }
            prepare(player, spawned, configurable);
            if (spawned instanceof LivingEntity living) {
                if (declared.raisesVictim() && summon.keepGear() && target != null) {
                    SummonEquipment.applyStacks(living, SummonLifecycle.gearOf(target));
                } else {
                    summon.equipment().applyTo(living);
                }
                summon.attributes().applyTo(living);
            }
            if (spawned instanceof SummonData data) {
                data.combat_styles$setSpectral(summon.spectral());
            }
            if (spawned instanceof Mob mob && summon.inheritTarget() && target != null
                    && SummonAi.huntable(mob, target)) {
                mob.setTarget(target);
            }
            level.addFreshEntity(spawned);
            SummonCaps.track(player, source, spawned);
            announce(level, player, spawned);
            int lifespan = lifespanFor(summon, profile);
            if (lifespan > 0 && (configurable == null || configurable.despawnsWithLifespan())) {
                DESPAWNING.add(new Despawn(spawned, level.getGameTime() + lifespan));
            }
            SummonLifecycle.spawned(player, spawned);
        }
    }

    public static Entity spawnCompanion(ServerLevel level, Player player, SummonProfile profile,
                                        SummonProfile.Resolved resolved, SummonCompanion companion,
                                        int slot, int count) {
        EntityType<?> type = typeOf(profile.entity().orElse(""));
        if (type == null) {
            return null;
        }
        Entity spawned = type.create(level);
        if (spawned == null) {
            return null;
        }
        ConfigurableSummon configurable = spawned instanceof ConfigurableSummon summoned
                ? summoned
                : null;
        double angle = FULL_TURN * slot / Math.max(1, count);
        double lift = configurable == null ? 0.0D : configurable.spawnLift();
        spawned.moveTo(player.getX() + Math.cos(angle) * COMPANION_RADIUS, player.getY() + lift,
                player.getZ() + Math.sin(angle) * COMPANION_RADIUS, player.getYRot(), 0.0F);
        float attack = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        DamageSpec spec = profile.damageSpec().orElse(null);
        float damage = spec != null ? spec.amountFor(player, attack) : attack * resolved.damageScale();
        String profileKey = SummonProfiles.keyOf(player, profile);
        if (configurable != null) {
            configurable.configure(player, null, damage, 0);
            configurable.companionSpawned(player, slot, count);
            configurable.bindProfile(profileKey, slot);
        } else if (spawned instanceof SummonData data && !profile.ownDamage()) {
            data.combat_styles$setSummonDamage(damage);
            data.combat_styles$setSummonSpec(spec);
        }
        if (configurable == null && spawned instanceof LivingEntity hosted
                && AbilityHosts.wants(profile)) {
            AbilityHosts.track(player, hosted, profileKey, slot, damage);
        }
        prepare(player, spawned, configurable);
        if (spawned instanceof LivingEntity living) {
            companion.equipment().applyTo(living);
            companion.attributes().applyTo(living);
        }
        if (spawned instanceof SummonData data) {
            data.combat_styles$setSpectral(companion.spectral());
            data.combat_styles$setFollow(companion.follow());
        }
        level.addFreshEntity(spawned);
        announce(level, player, spawned);
        SummonLifecycle.spawned(player, spawned);
        return spawned;
    }

    private static void announce(ServerLevel level, Player owner, Entity spawned) {
        int rgb = owner.getUUID().hashCode() & RGB_MASK;
        Vector3f color = new Vector3f(((rgb >> RED_SHIFT) & CHANNEL) / CHANNEL_MAX,
                ((rgb >> GREEN_SHIFT) & CHANNEL) / CHANNEL_MAX, (rgb & CHANNEL) / CHANNEL_MAX);
        level.sendParticles(new DustParticleOptions(color, PARTICLE_SCALE), spawned.getX(),
                spawned.getY() + spawned.getBbHeight() * HALF, spawned.getZ(), PARTICLE_COUNT,
                spawned.getBbWidth() * HALF, spawned.getBbHeight() * HALF,
                spawned.getBbWidth() * HALF, PARTICLE_SPEED);
    }

    private static void prepare(Player player, Entity spawned, ConfigurableSummon configurable) {
        SummonOwner.stamp(spawned, player);
        boolean selfDriven = SpellEngineCompat.prepareSummon(spawned, player);
        if (configurable == null && !selfDriven && spawned instanceof SummonData goals) {
            goals.combat_styles$applySummonGoals();
        }
        if (spawned instanceof Mob mob) {
            mob.setCustomName(Component.translatable(KEY_SUMMON_NAME, player.getDisplayName(),
                    mob.getType().getDescription()));
            mob.setCustomNameVisible(false);
        }
    }

    private static AffixEffects.Summon resolveVictim(AffixEffects.Summon summon, LivingEntity target) {
        if (!summon.raisesVictim()) {
            return summon;
        }
        if (target == null || !(target instanceof Mob) || SummonOwner.isSummon(target)
                || !raisable(target.getType())) {
            return null;
        }
        return summon.asEntity(idOf(target.getType()));
    }

    public static boolean raisable(EntityType<?> type) {
        return type != null && !NEVER_RAISED.contains(type) && !type.is(BOSSES);
    }

    private static SummonProfile profileFor(Player player, String entityId) {
        return RaiseProfile.INHERIT.profileFor(player, entityId);
    }

    private static EntityType<?> typeOf(String raw) {
        ResourceLocation id = ResourceLocation.tryParse(raw);
        return id == null ? null : BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null);
    }

    private static String idOf(EntityType<?> type) {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        return id == null ? "" : id.toString();
    }

    private static void place(ServerLevel level, Player player, Entity spawned,
                              ConfigurableSummon configurable, AffixEffects.Summon summon,
                              LivingEntity target, int index, int spread, double baseAngle) {
        double angle = configurable == null
                ? Double.NaN
                : configurable.formationAngle(index, spread);
        if (Double.isNaN(angle)) {
            angle = spread == 0
                    ? level.random.nextDouble() * FULL_TURN
                    : baseAngle + (FULL_TURN / spread) * index
                            + level.random.nextDouble() * ANGLE_JITTER;
        }
        double lift = configurable == null ? 0.0D : configurable.spawnLift();
        ProjectileSpawn placement = summon.spawn().orElse(null);
        if (placement != null) {
            Vec3 origin = placement.originFor(player, target, index, Math.max(1, spread));
            spawned.setPos(origin.x, origin.y + lift, origin.z);
            return;
        }
        double distance = SPAWN_MIN_RADIUS + Math.sqrt(level.random.nextDouble())
                * Math.max(0.0D, summon.radius() - SPAWN_MIN_RADIUS);
        spawned.setPos(player.getX() + Math.cos(angle) * distance,
                player.getY() + lift,
                player.getZ() + Math.sin(angle) * distance);
    }

    private static int lifespanFor(AffixEffects.Summon summon, SummonProfile profile) {
        return profile == null
                ? summon.lifespanTicks()
                : profile.lifespan().orElse(summon.lifespanTicks());
    }

    private static void configure(Player player, LivingEntity target,
                                  ConfigurableSummon configurable, AffixEffects.Summon summon,
                                  SummonProfile profile, float summonDamage, int index) {
        float damage = summonDamage * configurable.damageScale(player);
        configurable.configure(player, target, damage, lifespanFor(summon, profile));
        String ability = configurable.abilityFor(player, profile,
                summon.ability().orElse(null), index);
        SummonOrders.commandOne(player, configurable, target, ability, 0.0F, null);
    }

    private static boolean retargetExisting(ServerLevel level, Player player,
                                            LivingEntity target, AffixEffects.Summon summon,
                                            float damage) {
        if (summon.ability().isEmpty()) {
            return false;
        }
        AABB area = player.getBoundingBox().inflate(REUSE_SEARCH_RADIUS);
        for (Entity entity : level.getEntities(player, area, candidate ->
                candidate instanceof ConfigurableSummon)) {
            ConfigurableSummon existing = (ConfigurableSummon) entity;
            if (!existing.reusable() || entity.isRemoved()) {
                continue;
            }
            if (!SummonOwner.ownedBy(entity, player)) {
                continue;
            }
            existing.configure(player, target, damage, summon.lifespanTicks());
            String ability = summon.ability().orElse(null);
            if (ability != null) {
                SummonOrders.commandAll(player, target, ability, damage);
            }
            return true;
        }
        return false;
    }

    private static float summonDamage(Player player, AffixEffects.Summon summon, DamageSpec spec,
                                      float dealtDamage, AffixEffects.Scaling override) {
        if (spec != null) {
            return spec.amountFor(player, dealtDamage);
        }
        if (summon.damageRatio() <= 0.0F || dealtDamage <= 0.0F) {
            return 0.0F;
        }
        float ratio = summon.damageRatio();
        if (override != null) {
            ratio = override.apply(ratio,
                    SpellPowerAccess.valueOf(player, override.sources(), override.strongest()));
        }
        return dealtDamage * ratio;
    }
}
