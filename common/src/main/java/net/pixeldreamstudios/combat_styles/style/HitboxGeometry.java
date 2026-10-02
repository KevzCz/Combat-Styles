package net.pixeldreamstudios.combat_styles.style;

import net.bettercombat.api.WeaponAttributes;
import net.bettercombat.client.collision.OrientedBoundingBox;
import net.bettercombat.client.collision.WeaponHitBoxes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public final class HitboxGeometry {

    private static final double TRACING_DROP = 0.15D;
    private static final double SPIN_ANGLE = 180.0D;
    private static final double CURSOR_SWEEP = 1.0D;

    public record View(float yaw, float pitch) {

        public static View of(Player player) {
            return new View(player.getYRot(), player.getXRot());
        }
    }

    private HitboxGeometry() {
    }

    public static boolean spins(WeaponAttributes.Attack attack) {
        return attack.angle() > SPIN_ANGLE;
    }

    public static OrientedBoundingBox base(Player player, WeaponAttributes.Attack attack,
                                           double range) {
        return base(player, View.of(player), attack, range);
    }

    public static OrientedBoundingBox base(Player player, View view,
                                           WeaponAttributes.Attack attack, double range) {
        Vec3 origin = player.getEyePosition().subtract(0.0D,
                player.getBbHeight() * TRACING_DROP * player.getAgeScale(), 0.0D);
        boolean spin = spins(attack);
        Vec3 size = WeaponHitBoxes.createHitbox(attack.hitbox(), range, spin);
        OrientedBoundingBox box = new OrientedBoundingBox(origin, size,
                view.pitch(), view.yaw());
        if (!spin) {
            box = box.offsetAlongAxisZ(size.z / 2.0D);
        }
        return box.updateVertex();
    }

    public static OrientedBoundingBox moved(OrientedBoundingBox base, Player player,
                                            AttackBehaviour behaviour, boolean spin,
                                            SlashAnchor anchor, Entity cursorTarget) {
        return moved(base, player, View.of(player), behaviour, spin, anchor, cursorTarget);
    }

    public static OrientedBoundingBox moved(OrientedBoundingBox base, Player player, View view,
                                            AttackBehaviour behaviour, boolean spin,
                                            SlashAnchor anchor, Entity cursorTarget) {
        HitboxOffset offset = behaviour.offsetOrNone().clamped();
        AttackPlacement placement = behaviour.placementOrNone();
        float yaw = view.yaw() * Mth.DEG_TO_RAD;
        double sin = Math.sin(yaw);
        double cos = Math.cos(yaw);
        Vec3 shiftBy = new Vec3(
                offset.forward() * -sin + offset.right() * cos,
                offset.up(),
                offset.forward() * cos + offset.right() * sin);
        OrientedBoundingBox moved = offset.reorients()
                ? reoriented(base, view, offset, spin)
                : base.copy();
        if (placement.followsCursor()) {
            moved.center = cursorPoint(player, view, placement);
        } else if (anchor.tracksTarget() && cursorTarget != null) {
            moved.center = cursorTarget.position()
                    .add(0.0D, cursorTarget.getBbHeight() / 2.0D, 0.0D);
        } else if (anchor.worldAligned()) {
            moved.center = player.getEyePosition();
        }
        return moved.offset(shiftBy).updateVertex();
    }

    public static OrientedBoundingBox shaped(OrientedBoundingBox base, Player player,
                                             HitboxSet.Shape shape) {
        return shaped(base, View.of(player), shape);
    }

    public static OrientedBoundingBox shaped(OrientedBoundingBox base, View view,
                                             HitboxSet.Shape shape) {
        return shaped(shape, base.center, view.yaw(), view.pitch()).updateVertex();
    }

    public static OrientedBoundingBox shaped(HitboxSet.Shape shape, Vec3 origin,
                                             float bodyYaw, float bodyPitch) {
        return new OrientedBoundingBox(
                HitboxSet.centreOf(shape, origin, bodyYaw),
                HitboxSet.sizeOf(shape),
                HitboxSet.pitchOf(shape, bodyPitch),
                HitboxSet.yawOf(shape, bodyYaw));
    }

    public static OrientedBoundingBox reoriented(OrientedBoundingBox base, View view,
                                                 HitboxOffset offset, boolean centred) {
        float yaw = view.yaw() + offset.yaw();
        float pitch = view.pitch() + offset.pitch();
        Vec3 size = base.extent.scale(2.0D);
        double push = centred ? 0.0D : base.extent.z;
        Vec3 anchored = base.center.subtract(base.axisZ.scale(push));
        OrientedBoundingBox rebuilt = new OrientedBoundingBox(anchored, size, pitch, yaw);
        if (push != 0.0D) {
            rebuilt.center = anchored.add(rebuilt.axisZ.normalize().scale(push));
        }
        rebuilt = rebuilt.updateVertex();

        float roll = offset.roll();
        if (roll != 0.0F) {
            float radians = roll * Mth.DEG_TO_RAD;
            double sin = Math.sin(radians);
            double cos = Math.cos(radians);
            Vec3 rolledX = rebuilt.axisX.scale(cos).add(rebuilt.axisY.scale(sin));
            Vec3 rolledY = rebuilt.axisY.scale(cos).subtract(rebuilt.axisX.scale(sin));
            rebuilt.axisX = rolledX;
            rebuilt.axisY = rolledY;
            rebuilt = rebuilt.updateVertex();
        }
        return rebuilt;
    }

    public static Vec3 cursorPoint(Player player, View view, AttackPlacement placement) {
        Vec3 from = player.getEyePosition();
        Vec3 look = Vec3.directionFromRotation(view.pitch(), view.yaw());
        double reach = placement.clampedRange();
        Vec3 to = from.add(look.scale(reach));

        BlockHitResult blockHit = player.level().clip(new ClipContext(from, to,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        double best = blockHit.getType() == HitResult.Type.BLOCK
                ? blockHit.getLocation().distanceTo(from)
                : reach;

        if (placement.stopAtEntities()) {
            AABB sweep = new AABB(from, to).inflate(CURSOR_SWEEP);
            for (Entity candidate : player.level().getEntities(player, sweep,
                    entity -> entity.isPickable() && entity.isAlive())) {
                Optional<Vec3> clip = candidate.getBoundingBox()
                        .inflate(candidate.getPickRadius())
                        .clip(from, to);
                if (clip.isEmpty()) {
                    continue;
                }
                double distance = clip.get().distanceTo(from);
                if (distance < best) {
                    best = distance;
                }
            }
        }

        double stopped = Math.max(0.0D, best - placement.stopMargin());
        return from.add(look.scale(stopped));
    }
}
