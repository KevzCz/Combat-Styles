package net.pixeldreamstudios.combat_styles.entity;

import net.pixeldreamstudios.combat_styles.damage.DamageDealer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.entity.projectile.Projectile;

import net.pixeldreamstudios.combat_styles.affix.AffixDamageDealer;
import net.pixeldreamstudios.combat_styles.debug.CombatDebug;
import net.pixeldreamstudios.combat_styles.equipment.ProjectileImpactScope;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;

public final class SummonStrike {

    private static final ThreadLocal<Boolean> STRIKING =
            ThreadLocal.withInitial(() -> Boolean.FALSE);

    private static final ThreadLocal<Entity> STRIKER = new ThreadLocal<>();

    private static final ThreadLocal<Deque<Entity>> CASTING =
            ThreadLocal.withInitial(ArrayDeque::new);

    private SummonStrike() {
    }

    public static boolean deal(LivingEntity target, DamageSource source, float amount) {
        if (STRIKING.get()) {
            return false;
        }
        Entity summon = summonBehind(source);
        if (summon == null || summon == target) {
            return false;
        }
        if (CombatDebug.enabled(nearbyOwner(summon))) {
            CombatDebug.note(nearbyOwner(summon), SUMMON_DEBUG + " source="
                    + source.getMsgId() + " direct=" + describe(source.getDirectEntity())
                    + " causing=" + describe(source.getEntity())
                    + " summon=" + describe(summon));
        }
        SummonData data = (SummonData) summon;
        float damage = data.combat_styles$summonDamage();
        if (damage <= 0.0F) {
            if (CombatDebug.enabled(nearbyOwner(summon))) {
                CombatDebug.note(nearbyOwner(summon), SUMMON_DEBUG
                        + " skipped: no stored damage on " + describe(summon));
            }
            return false;
        }
        Player owner = ownerOf(summon, data);
        if (owner == null || owner == target) {
            return false;
        }
        STRIKING.set(Boolean.TRUE);
        STRIKER.set(summon);
        try {
            DamageDealer.dealSpec(owner, target, data.combat_styles$summonSpec(),
                    damage, source);
            AffixDamageDealer.onSummonHit(owner, target, damage);
        } finally {
            STRIKING.set(Boolean.FALSE);
            STRIKER.remove();
        }
        return true;
    }

    public static Entity striking() {
        return STRIKER.get();
    }

    public static void beginCast(Entity caster) {
        if (caster != null && !caster.level().isClientSide()) {
            CASTING.get().push(caster);
        }
    }

    public static void endCast(Entity caster) {
        if (caster != null && !caster.level().isClientSide()) {
            CASTING.get().remove(caster);
        }
    }

    public static void linkCast(Entity spawned) {
        Entity caster = asSummon(CASTING.get().peek());
        if (caster != null && !(spawned instanceof Mob) && spawned instanceof SummonData data) {
            data.combat_styles$setCastBy(caster.getUUID());
        }
    }

    private static Entity castingFor(DamageSource source) {
        Entity caster = asSummon(CASTING.get().peek());
        if (caster == null) {
            return null;
        }
        Entity attacker = source.getEntity();
        return attacker != null && (attacker == caster
                || attacker.getUUID().equals(((SummonData) caster).combat_styles$summonOwner()))
                ? caster
                : null;
    }

    private static Entity castBy(Entity carrier) {
        if (!(carrier instanceof SummonData data) || data.combat_styles$castBy() == null
                || !(carrier.level() instanceof ServerLevel server)) {
            return null;
        }
        return asSummon(server.getEntity(data.combat_styles$castBy()));
    }

    public static boolean shieldsOwner(LivingEntity target, DamageSource source) {
        Entity summon = summonBehind(source);
        if (summon == null || summon == target) {
            return false;
        }
        return target.getUUID().equals(((SummonData) summon).combat_styles$summonOwner());
    }

    private static final String SUMMON_DEBUG = "[summon]";

    private static String describe(Entity entity) {
        return entity == null ? "none" : entity.getType().toShortString();
    }

    private static Player nearbyOwner(Entity summon) {
        UUID owner = summon instanceof SummonData data
                ? data.combat_styles$summonOwner()
                : null;
        return owner == null ? null : summon.level().getPlayerByUUID(owner);
    }

    private static Entity summonBehind(DamageSource source) {
        Entity direct = asSummon(source.getDirectEntity());
        if (direct != null) {
            return direct;
        }
        Entity causing = asSummon(source.getEntity());
        if (causing != null) {
            return causing;
        }
        Entity carrier = source.getDirectEntity();
        if (carrier instanceof Projectile projectile) {
            Entity shooter = asSummon(projectile.getOwner());
            if (shooter != null) {
                return shooter;
            }
        }
        if (carrier instanceof TraceableEntity traceable) {
            Entity caster = asSummon(traceable.getOwner());
            if (caster != null) {
                return caster;
            }
        }
        if (carrier instanceof OwnableEntity ownable) {
            Entity owned = asSummon(ownerEntity(carrier, ownable));
            if (owned != null) {
                return owned;
            }
        }
        Entity casting = castingFor(source);
        if (casting != null) {
            return casting;
        }
        Entity linked = castBy(carrier);
        if (linked != null) {
            return linked;
        }
        linked = castBy(ProjectileImpactScope.landingFor(source));
        return linked != null ? linked : castBy(ProjectileImpactScope.carrierFor(source));
    }

    private static Entity ownerEntity(Entity carrier, OwnableEntity ownable) {
        UUID owner = ownable.getOwnerUUID();
        if (owner == null || !(carrier.level() instanceof ServerLevel server)) {
            return null;
        }
        return server.getEntity(owner);
    }

    private static Entity asSummon(Entity entity) {
        if (!(entity instanceof SummonData data)) {
            return null;
        }
        return data.combat_styles$summonOwner() == null ? null : entity;
    }

    private static Player ownerOf(Entity summon, SummonData data) {
        return summon.level().getPlayerByUUID(data.combat_styles$summonOwner());
    }
}
