package net.pixeldreamstudios.combat_styles.debug;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import net.pixeldreamstudios.combat_styles.affix.ProjectileUpkeep;

import java.util.Comparator;
import java.util.Locale;

public final class SpellShotTrace {

    private static final double NEAR_RADIUS = 3.0D;
    private static final String VECTOR_FORMAT = "(%.2f, %.2f, %.2f)";
    private static final String NUMBER_FORMAT = "%.2f";
    private static final String TRAVEL_FORMAT = "%.1f";

    private SpellShotTrace() {
    }

    public static void flight(Projectile projectile) {
        if (projectile == null || projectile.level().isClientSide()) {
            return;
        }
        ProjectileUpkeep.SpellShotSpec shot = ProjectileUpkeep.spellShotFor(projectile);
        if (shot == null || !CombatDebug.enabled(shot.owner())) {
            return;
        }
        Vec3 position = projectile.position();
        LivingEntity near = projectile.level().getEntitiesOfClass(LivingEntity.class,
                        projectile.getBoundingBox().inflate(NEAR_RADIUS),
                        entity -> entity != shot.owner() && entity.isAlive())
                .stream()
                .min(Comparator.comparingDouble(entity -> entity.getBoundingBox()
                        .distanceToSqr(position)))
                .orElse(null);
        if (near == null) {
            return;
        }
        AABB box = near.getBoundingBox();
        Vec3 motion = projectile.getDeltaMovement();
        CombatDebug.note(shot.owner(), "spellshot — flight age " + projectile.tickCount
                + " pos " + vector(position) + " vel " + vector(motion)
                + " |v| " + number(motion.length())
                + " rot " + number(projectile.getYRot()) + "/" + number(projectile.getXRot())
                + " bb " + number(projectile.getBbWidth()) + "x" + number(projectile.getBbHeight())
                + " near " + near.getName().getString()
                + " gap " + number(Math.sqrt(box.distanceToSqr(position)))
                + " y " + number(box.minY) + ".." + number(box.maxY)
                + " moved " + number(projectile.moveDist));
    }

    public static void impact(Projectile projectile, HitResult result) {
        if (projectile == null || projectile.level().isClientSide()) {
            return;
        }
        ProjectileUpkeep.SpellShotSpec shot = ProjectileUpkeep.spellShotFor(projectile);
        if (shot == null || !CombatDebug.enabled(shot.owner())) {
            return;
        }
        String struck = result instanceof EntityHitResult entityHit
                ? " " + entityHit.getEntity().getName().getString()
                : "";
        CombatDebug.note(shot.owner(), "spellshot — onHit " + result.getType() + struck
                + " travelled " + travel(projectile));
    }

    public static void removed(Projectile projectile) {
        if (projectile == null || !projectile.isRemoved() || projectile.level().isClientSide()) {
            return;
        }
        ProjectileUpkeep.SpellShotSpec shot = ProjectileUpkeep.spellShotFor(projectile);
        if (shot == null || !CombatDebug.enabled(shot.owner())) {
            return;
        }
        CombatDebug.note(shot.owner(), "spellshot — removed " + projectile.getRemovalReason()
                + " travelled " + travel(projectile) + " age " + projectile.tickCount);
    }

    private static String travel(Projectile projectile) {
        return String.format(Locale.ROOT, TRAVEL_FORMAT, projectile.moveDist);
    }

    private static String vector(Vec3 value) {
        return String.format(Locale.ROOT, VECTOR_FORMAT, value.x, value.y, value.z);
    }

    private static String number(double value) {
        return String.format(Locale.ROOT, NUMBER_FORMAT, value);
    }
}
