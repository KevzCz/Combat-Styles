package net.pixeldreamstudios.combat_styles.affix;

import net.pixeldreamstudios.combat_styles.equipment.UseScope;

import net.pixeldreamstudios.combat_styles.damage.DamageSpec;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import net.pixeldreamstudios.combat_styles.compat.SpellEngineCompat;
import net.pixeldreamstudios.combat_styles.custom.CustomScopes;
import net.pixeldreamstudios.combat_styles.custom.Customs;
import net.pixeldreamstudios.combat_styles.entity.SummonAi;

import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class ProjectileUpkeep {

    private record Tracked(Entity entity, int expiresAtAge, boolean removeOnLanding,
                           DamageSpec spec, Player owner, ProjectileVelocity velocity,
                           boolean ignoresIframes, float triggering, float carried,
                           boolean viaSummon, Object box) {
    }

    private static final Set<UUID> LANDED = new HashSet<>();

    public static final float NO_CARRIED = -1.0F;

    public record SpellShotSpec(Player owner, DamageSpec spec, boolean ignoresIframes) {

        public boolean retypes() {
            return spec != null && spec.retypes();
        }
    }

    private record SpellShotTag(WeakReference<Object> tag, SpellShotSpec shot) {
    }

    private static final List<SpellShotTag> SPELL_SHOTS = new ArrayList<>();
    private static final Deque<SpellShotSpec> CASTING = new ArrayDeque<>();

    private static final double HOMING_RADIUS = 12.0D;
    private static final int NEVER_EXPIRES = Integer.MAX_VALUE;
    private static final int SETTLE_TICKS = 2;
    private static final double STOPPED_SPEED = 1.0E-4D;

    private static final Map<UUID, Tracked> TRACKED = new LinkedHashMap<>();

    private ProjectileUpkeep() {
    }

    public static void track(Player owner, Entity entity, AffixEffects.Projectile projectile,
                             float triggering, float carried, boolean viaSummon) {
        DamageSpec spec = projectile.damageSpec().orElse(null);
        boolean meaningful = spec != null && !spec.isNeutral();
        boolean carries = carried > 0.0F;
        ProjectileVelocity velocity = projectile.spawn().velocity();
        boolean steers = velocity.steersInFlight();
        boolean hooked = Customs.has(projectile);
        if (entity == null || (!projectile.hasLifetime() && !projectile.removeOnLanding()
                && !meaningful && !carries && !steers && !projectile.ignoreIframes() && !hooked)) {
            return;
        }
        UseScope.capture(entity);
        int expiresAt = projectile.hasLifetime()
                ? entity.tickCount + projectile.lifetimeTicks()
                : NEVER_EXPIRES;
        TRACKED.put(entity.getUUID(), new Tracked(entity, expiresAt,
                projectile.removeOnLanding(), meaningful ? spec : null, owner,
                steers ? velocity : null, projectile.ignoreIframes(), triggering,
                carries ? carried : NO_CARRIED, viaSummon, hooked ? projectile : null));
    }

    public static void trackLike(Entity source, Entity copy) {
        UseScope.inherit(source, copy);
        Tracked tracked = source == null || copy == null ? null : TRACKED.get(source.getUUID());
        if (tracked == null) {
            return;
        }
        int remaining = tracked.expiresAtAge() == NEVER_EXPIRES
                ? NEVER_EXPIRES
                : copy.tickCount + Math.max(0, tracked.expiresAtAge() - source.tickCount);
        TRACKED.put(copy.getUUID(), new Tracked(copy, remaining, tracked.removeOnLanding(),
                tracked.spec(), tracked.owner(), tracked.velocity(), tracked.ignoresIframes(),
                tracked.triggering(), tracked.carried(), tracked.viaSummon(), tracked.box()));
    }

    public static Object boxFor(Entity projectile) {
        Tracked tracked = projectile == null ? null : TRACKED.get(projectile.getUUID());
        return tracked == null ? null : tracked.box();
    }

    public static boolean dealsOwnDamage(Entity projectile) {
        Tracked tracked = projectile == null ? null : TRACKED.get(projectile.getUUID());
        return tracked != null && (tracked.spec() != null || tracked.carried() > 0.0F);
    }

    public static float triggeringFor(Entity projectile) {
        Tracked tracked = projectile == null ? null : TRACKED.get(projectile.getUUID());
        return tracked == null ? 0.0F : tracked.triggering();
    }

    public static float carriedFor(Entity projectile) {
        Tracked tracked = projectile == null ? null : TRACKED.get(projectile.getUUID());
        return tracked == null ? NO_CARRIED : tracked.carried();
    }

    public static boolean launchedBySummon(Entity projectile) {
        Tracked tracked = projectile == null ? null : TRACKED.get(projectile.getUUID());
        return tracked != null && tracked.viaSummon();
    }

    public static void watchSpellShot(Object tag, Player owner, DamageSpec spec,
                                      boolean ignoresIframes) {
        SPELL_SHOTS.removeIf(entry -> entry.tag().get() == null);
        if (tag == null || owner == null) {
            return;
        }
        SPELL_SHOTS.add(new SpellShotTag(new WeakReference<>(tag),
                new SpellShotSpec(owner, spec, ignoresIframes)));
    }

    public static void beginCast(Player owner, DamageSpec spec, boolean ignoresIframes) {
        CASTING.push(new SpellShotSpec(owner, spec, ignoresIframes));
    }

    public static void endCast() {
        CASTING.poll();
    }

    public static SpellShotSpec castingShotFor(DamageSource source) {
        SpellShotSpec casting = CASTING.peek();
        if (casting == null || source == null) {
            return null;
        }
        return source.getEntity() == casting.owner() ? casting : null;
    }

    public static boolean ignoresIframes(Entity projectile) {
        if (projectile == null) {
            return false;
        }
        Tracked tracked = TRACKED.get(projectile.getUUID());
        if (tracked != null) {
            return tracked.ignoresIframes();
        }
        SpellShotSpec shot = spellShotFor(projectile);
        return shot != null && shot.ignoresIframes();
    }

    public static SpellShotSpec spellShotFor(Entity projectile) {
        if (SPELL_SHOTS.isEmpty()) {
            return null;
        }
        Object tag = SpellEngineCompat.shotTagOf(projectile);
        if (tag == null) {
            return null;
        }
        for (SpellShotTag entry : SPELL_SHOTS) {
            if (entry.tag().get() == tag) {
                return entry.shot();
            }
        }
        return null;
    }

    public static DamageSpec specFor(Entity projectile) {
        if (projectile == null) {
            return null;
        }
        Tracked tracked = TRACKED.get(projectile.getUUID());
        return tracked == null ? null : tracked.spec();
    }

    public static Player ownerFor(Entity projectile) {
        if (projectile == null) {
            return null;
        }
        Tracked tracked = TRACKED.get(projectile.getUUID());
        return tracked == null ? null : tracked.owner();
    }

    public static void tick() {
        if (TRACKED.isEmpty()) {
            return;
        }
        List<UUID> finished = new ArrayList<>();
        for (Map.Entry<UUID, Tracked> entry : TRACKED.entrySet()) {
            Tracked tracked = entry.getValue();
            Entity entity = tracked.entity();
            if (entity == null || entity.isRemoved()) {
                finished.add(entry.getKey());
                continue;
            }
            if (tracked.box() != null) {
                Customs.ticked(CustomScopes.PROJECTILE, tracked.box(), tracked.owner(), entity);
                if (hasLanded(entity) && LANDED.add(entry.getKey())) {
                    Customs.landed(CustomScopes.PROJECTILE, tracked.box(), tracked.owner(), entity);
                }
            }
            if (tracked.velocity() != null) {
                tracked.velocity().applyTo(entity, homingTarget(entity, tracked.owner()));
            }
            if (tracked.removeOnLanding() && hasLanded(entity)) {
                entity.discard();
                finished.add(entry.getKey());
                continue;
            }
            if (entity.tickCount >= tracked.expiresAtAge()) {
                entity.discard();
                finished.add(entry.getKey());
            }
        }
        for (UUID id : finished) {
            LANDED.remove(id);
            TRACKED.remove(id);
        }
    }

    private static Entity homingTarget(Entity projectile, Player owner) {
        if (projectile == null || projectile.level() == null) {
            return null;
        }
        AABB search = projectile.getBoundingBox().inflate(HOMING_RADIUS);
        Entity closest = null;
        double nearest = Double.MAX_VALUE;
        for (Entity candidate : projectile.level().getEntities(projectile, search,
                entity -> entity instanceof LivingEntity living && entity.isAlive()
                        && entity != owner && homable(owner, living))) {
            double distance = candidate.distanceToSqr(projectile);
            if (distance < nearest) {
                nearest = distance;
                closest = candidate;
            }
        }
        return closest;
    }

    private static boolean homable(Player owner, LivingEntity candidate) {
        if (owner == null) {
            return true;
        }
        if (candidate == owner || SummonAi.ownedBy(candidate, owner.getUUID())) {
            return false;
        }
        return AffixTargets.canHit(owner, candidate);
    }

    private static boolean hasLanded(Entity entity) {
        if (entity.onGround() || entity.horizontalCollision || entity.verticalCollision) {
            return true;
        }
        return entity.tickCount > SETTLE_TICKS
                && entity.getDeltaMovement().lengthSqr() < STOPPED_SPEED;
    }

    public static void untrack(Entity projectile) {
        if (projectile != null) {
            TRACKED.remove(projectile.getUUID());
            LANDED.remove(projectile.getUUID());
        }
    }

    public static void clear() {
        TRACKED.clear();
        LANDED.clear();
        SPELL_SHOTS.clear();
    }
}
