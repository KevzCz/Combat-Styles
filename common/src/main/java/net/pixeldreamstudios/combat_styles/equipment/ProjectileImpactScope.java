package net.pixeldreamstudios.combat_styles.equipment;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.entity.projectile.Projectile;

import net.pixeldreamstudios.combat_styles.compat.SpellEngineCompat;

import java.util.ArrayDeque;
import java.util.Deque;

public final class ProjectileImpactScope {

    private static final Deque<Projectile> LANDING = new ArrayDeque<>();
    private static final Deque<Entity> CARRIERS = new ArrayDeque<>();

    private ProjectileImpactScope() {
    }

    public static void enter(Projectile projectile) {
        if (projectile == null || projectile.level().isClientSide()) {
            return;
        }
        LANDING.push(projectile);
    }

    public static void exit(Projectile projectile) {
        if (projectile == null || projectile.level().isClientSide()) {
            return;
        }
        LANDING.remove(projectile);
    }

    public static void enterCarrier(Entity carrier) {
        if (carrier == null || carrier.level().isClientSide()) {
            return;
        }
        CARRIERS.push(carrier);
    }

    public static void exitCarrier(Entity carrier) {
        if (carrier == null || carrier.level().isClientSide()) {
            return;
        }
        CARRIERS.remove(carrier);
    }

    public static Entity carrierFor(DamageSource source) {
        Entity carrier = CARRIERS.peek();
        if (!(carrier instanceof TraceableEntity traced)) {
            return null;
        }
        Entity attacker = source.getEntity();
        return attacker != null && attacker == traced.getOwner() ? carrier : null;
    }

    public static Projectile landingFor(DamageSource source) {
        Entity direct = source.getDirectEntity();
        if (direct instanceof Projectile shot) {
            return shot;
        }
        Projectile landing = LANDING.peek();
        if (landing == null) {
            return null;
        }
        Entity attacker = source.getEntity();
        return attacker != null && attacker == landing.getOwner() ? landing : null;
    }

    public static Projectile hitBy(DamageSource source) {
        Projectile landing = landingFor(source);
        if (landing == null || source.getDirectEntity() == landing
                || SpellEngineCompat.isSpellProjectile(landing)) {
            return landing;
        }
        return null;
    }
}
