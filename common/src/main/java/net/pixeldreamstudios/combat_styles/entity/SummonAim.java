package net.pixeldreamstudios.combat_styles.entity;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import net.pixeldreamstudios.combat_styles.affix.AffixTargets;
import net.pixeldreamstudios.combat_styles.config.SummonTuning;

import java.util.Locale;

public enum SummonAim implements StringRepresentable {

    HIT_TARGET,
    LOOK,
    NEAREST,
    LOWEST_HEALTH,
    ATTACKER,
    OWNER;

    public static final Codec<SummonAim> CODEC = StringRepresentable.fromEnum(SummonAim::values);

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public LivingEntity resolve(Player owner, Entity from, LivingEntity hit) {
        if (owner == null) {
            return hit;
        }
        double range = Math.max(0.0D, SummonTuning.get().aimRange);
        Entity origin = from == null ? owner : from;
        return switch (this) {
            case HIT_TARGET -> usable(owner, origin, hit, range)
                    ? hit
                    : usableOrNull(owner, origin, owner.getLastHurtMob(), range);
            case LOOK -> looked(owner, range);
            case NEAREST -> nearest(owner, origin, range, false);
            case LOWEST_HEALTH -> nearest(owner, origin, range, true);
            case ATTACKER -> usableOrNull(owner, origin, owner.getLastHurtByMob(), range);
            case OWNER -> owner;
        };
    }

    public boolean targetsFoes() {
        return this != OWNER;
    }

    private static LivingEntity usableOrNull(Player owner, Entity origin, LivingEntity candidate,
                                             double range) {
        return usable(owner, origin, candidate, range) ? candidate : null;
    }

    private static boolean usable(Player owner, Entity origin, LivingEntity candidate,
                                  double range) {
        return candidate != null
                && candidate.isAlive()
                && !candidate.isRemoved()
                && candidate.distanceToSqr(origin) <= range * range
                && AffixTargets.canHit(owner, candidate);
    }

    private static LivingEntity looked(Player owner, double range) {
        Vec3 eye = owner.getEyePosition();
        Vec3 end = eye.add(owner.getLookAngle().scale(range));
        AABB area = owner.getBoundingBox().expandTowards(owner.getLookAngle().scale(range))
                .inflate(1.0D);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(owner, eye, end, area,
                candidate -> candidate instanceof LivingEntity living
                        && AffixTargets.canHit(owner, living),
                range * range);
        return hit != null && hit.getEntity() instanceof LivingEntity living ? living : null;
    }

    private static LivingEntity nearest(Player owner, Entity origin, double range,
                                        boolean weakest) {
        LivingEntity best = null;
        double score = Double.MAX_VALUE;
        for (LivingEntity candidate : origin.level().getEntitiesOfClass(LivingEntity.class,
                origin.getBoundingBox().inflate(range))) {
            if (!usable(owner, origin, candidate, range)) {
                continue;
            }
            double value = weakest ? candidate.getHealth() : candidate.distanceToSqr(origin);
            if (value < score) {
                score = value;
                best = candidate;
            }
        }
        return best;
    }
}
