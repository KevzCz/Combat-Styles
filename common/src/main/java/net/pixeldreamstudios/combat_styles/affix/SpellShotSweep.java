package net.pixeldreamstudios.combat_styles.affix;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import net.pixeldreamstudios.combat_styles.debug.CombatDebug;
import net.pixeldreamstudios.combat_styles.mixin.ProjectileHitInvoker;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public final class SpellShotSweep {

    private static final double MIN_TRAVEL_SQR = 1.0E-4D;
    private static final double SWEEP_MARGIN = 0.3D;

    private SpellShotSweep() {
    }

    public static void sweep(Projectile projectile) {
        if (projectile == null || projectile.isRemoved() || projectile.level().isClientSide()) {
            return;
        }
        ProjectileUpkeep.SpellShotSpec shot = ProjectileUpkeep.spellShotFor(projectile);
        if (shot == null) {
            return;
        }
        Vec3 from = new Vec3(projectile.xo, projectile.yo, projectile.zo);
        Vec3 to = projectile.position();
        Vec3 travel = to.subtract(from);
        if (travel.lengthSqr() < MIN_TRAVEL_SQR) {
            return;
        }
        ProjectileHitInvoker invoker = (ProjectileHitInvoker) projectile;
        AABB swept = projectile.getBoundingBox().move(from.subtract(to)).expandTowards(travel)
                .inflate(SWEEP_MARGIN);
        EntityHitResult crossed = ProjectileUtil.getEntityHitResult(projectile.level(), projectile,
                from, to, swept, entity -> entity instanceof LivingEntity
                        && invoker.combat_styles$canHitEntity(entity)
                        && AffixTargets.canHit(shot.owner(), entity));
        if (crossed == null || !SWEPT.computeIfAbsent(projectile, ignored -> new HashSet<>())
                .add(crossed.getEntity().getId())) {
            return;
        }
        if (CombatDebug.enabled(shot.owner())) {
            CombatDebug.note(shot.owner(), "spellshot — sweep crossed "
                    + crossed.getEntity().getName().getString());
        }
        invoker.combat_styles$onHit(crossed);
    }

    private static final Map<Projectile, Set<Integer>> SWEPT = new WeakHashMap<>();
}
