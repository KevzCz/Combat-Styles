package net.pixeldreamstudios.combat_styles.affix;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import net.pixeldreamstudios.combat_styles.custom.Customs;

public record ProjectileSpawn(SpawnAnchor anchor, float forward, float up, float right,
                              float yaw, float pitch, SpreadMode mode, float arc,
                              float inheritMotion, boolean aimAtTarget,
                              ProjectileVelocity velocity) {

    public static final float OFFSET_LIMIT = 16.0F;
    public static final float ANGLE_LIMIT = 180.0F;
    public static final float ARC_LIMIT = 360.0F;
    private static final double EYE_RATIO = 0.8D;
    private static final double TARGET_RATIO = 0.5D;
    private static final double HAND_SIDE = 0.35D;
    private static final double HAND_DROP = 0.2D;

    public static final ProjectileSpawn DEFAULT = new ProjectileSpawn(
            SpawnAnchor.EYES, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F,
            SpreadMode.RANDOM, 0.0F, 0.0F, false, ProjectileVelocity.DEFAULT);

    public static final Codec<ProjectileSpawn> CODEC = Customs.wrap(ProjectileSpawn.class, RecordCodecBuilder.create(instance -> instance.group(
            SpawnAnchor.CODEC.optionalFieldOf("anchor", SpawnAnchor.EYES)
                    .forGetter(ProjectileSpawn::anchor),
            Codec.FLOAT.optionalFieldOf("forward", 0.0F).forGetter(ProjectileSpawn::forward),
            Codec.FLOAT.optionalFieldOf("up", 0.0F).forGetter(ProjectileSpawn::up),
            Codec.FLOAT.optionalFieldOf("right", 0.0F).forGetter(ProjectileSpawn::right),
            Codec.FLOAT.optionalFieldOf("yaw", 0.0F).forGetter(ProjectileSpawn::yaw),
            Codec.FLOAT.optionalFieldOf("pitch", 0.0F).forGetter(ProjectileSpawn::pitch),
            SpreadMode.CODEC.optionalFieldOf("mode", SpreadMode.RANDOM)
                    .forGetter(ProjectileSpawn::mode),
            Codec.FLOAT.optionalFieldOf("arc", 0.0F).forGetter(ProjectileSpawn::arc),
            Codec.FLOAT.optionalFieldOf("inherit_motion", 0.0F)
                    .forGetter(ProjectileSpawn::inheritMotion),
            Codec.BOOL.optionalFieldOf("aim_at_target", false)
                    .forGetter(ProjectileSpawn::aimAtTarget),
            ProjectileVelocity.CODEC.optionalFieldOf("velocity", ProjectileVelocity.DEFAULT)
                    .forGetter(ProjectileSpawn::velocity)
    ).apply(instance, ProjectileSpawn::new)));

    public ProjectileSpawn {
        anchor = anchor == null ? SpawnAnchor.EYES : anchor;
        mode = mode == null ? SpreadMode.RANDOM : mode;
        velocity = velocity == null ? ProjectileVelocity.DEFAULT : velocity;
        forward = Math.clamp(forward, -OFFSET_LIMIT, OFFSET_LIMIT);
        up = Math.clamp(up, -OFFSET_LIMIT, OFFSET_LIMIT);
        right = Math.clamp(right, -OFFSET_LIMIT, OFFSET_LIMIT);
        yaw = Math.clamp(yaw, -ANGLE_LIMIT, ANGLE_LIMIT);
        pitch = Math.clamp(pitch, -ANGLE_LIMIT, ANGLE_LIMIT);
        arc = Math.clamp(arc, -ARC_LIMIT, ARC_LIMIT);
        inheritMotion = Math.clamp(inheritMotion, 0.0F, 1.0F);
    }

    public boolean isEmpty() {
        return anchor == SpawnAnchor.EYES && forward == 0.0F && up == 0.0F && right == 0.0F
                && yaw == 0.0F && pitch == 0.0F && mode == SpreadMode.RANDOM
                && arc == 0.0F && inheritMotion == 0.0F && !aimAtTarget
                && velocity.isEmpty();
    }

    public boolean placesOrigin() {
        return anchor != SpawnAnchor.EYES || forward != 0.0F || up != 0.0F || right != 0.0F
                || (mode.offsetsOrigin() && arc != 0.0F);
    }

    public Vec3 originFor(Player player, Entity target, int index, int count) {
        Vec3 base = anchorPoint(player, target);
        float bodyYaw = player.getYRot();
        Vec3 placed = base.add(local(forward, up, right, bodyYaw));
        if (mode.offsetsOrigin() && count > 1) {
            double step = arc / (double) Math.max(1, count - 1);
            double sideways = -arc / 2.0D + step * index;
            return placed.add(local(0.0F, 0.0F, (float) sideways, bodyYaw));
        }
        return placed;
    }

    public Vec3 directionFor(Player player, Entity target, Vec3 baseDirection,
                             int index, int count) {
        Vec3 aimed = baseDirection;
        if (aimAtTarget && target != null && target.isAlive()) {
            aimed = target.position().add(0.0D, target.getBbHeight() * TARGET_RATIO, 0.0D)
                    .subtract(player.getEyePosition()).normalize();
        }
        float spin = yaw;
        float tilt = pitch;
        if (mode.spacesEvenly() && count > 1) {
            double step = arc / (double) (mode == SpreadMode.RING ? count : count - 1);
            double offset = -arc / 2.0D + step * index;
            if (mode == SpreadMode.RING) {
                tilt += (float) (Math.sin(Math.toRadians(offset)) * arc / 2.0D);
                spin += (float) (Math.cos(Math.toRadians(offset)) * arc / 2.0D);
            } else {
                spin += (float) offset;
            }
        }
        return rotated(aimed, spin, tilt);
    }

    public Vec3 carriedMotion(Player player) {
        return inheritMotion <= 0.0F
                ? Vec3.ZERO
                : player.getDeltaMovement().scale(inheritMotion);
    }

    private Vec3 anchorPoint(Player player, Entity target) {
        return switch (anchor) {
            case TARGET -> target == null || !target.isAlive()
                    ? player.getEyePosition()
                    : target.position().add(0.0D, target.getBbHeight() * TARGET_RATIO, 0.0D);
            case FEET -> player.position();
            case PLAYER -> player.position().add(0.0D, player.getBbHeight() * TARGET_RATIO, 0.0D);
            case MAIN_HAND -> handPoint(player, true);
            case OFF_HAND -> handPoint(player, false);
            default -> player.position().add(0.0D, player.getEyeHeight() * EYE_RATIO, 0.0D);
        };
    }

    private Vec3 handPoint(Player player, boolean main) {
        boolean rightSide = main != (player.getMainArm() == HumanoidArm.LEFT);
        Vec3 eye = player.position().add(0.0D, player.getEyeHeight() * EYE_RATIO - HAND_DROP, 0.0D);
        return eye.add(local(0.0F, 0.0F, (float) (rightSide ? HAND_SIDE : -HAND_SIDE),
                player.getYRot()));
    }

    private static Vec3 local(float forwardAmount, float upAmount, float rightAmount,
                              float bodyYaw) {
        float radians = bodyYaw * Mth.DEG_TO_RAD;
        double sin = Math.sin(radians);
        double cos = Math.cos(radians);
        return new Vec3(
                forwardAmount * -sin + rightAmount * cos,
                upAmount,
                forwardAmount * cos + rightAmount * sin);
    }

    private static Vec3 rotated(Vec3 direction, float spin, float tilt) {
        if (spin == 0.0F && tilt == 0.0F) {
            return direction;
        }
        double length = direction.length();
        if (length < 1.0E-6D) {
            return direction;
        }
        Vec3 unit = direction.scale(1.0D / length);
        float currentYaw = (float) -Math.toDegrees(Math.atan2(unit.x, unit.z));
        float currentPitch = (float) -Math.toDegrees(Math.asin(Mth.clamp(unit.y, -1.0D, 1.0D)));
        return Vec3.directionFromRotation(currentPitch + tilt, currentYaw + spin).normalize();
    }

    public String signature() {
        if (isEmpty()) {
            return "";
        }
        return "s" + anchor.getSerializedName() + forward + "_" + up + "_" + right
                + "_" + yaw + "_" + pitch + mode.getSerializedName() + arc
                + (inheritMotion == 0.0F ? "" : "m" + inheritMotion)
                + (aimAtTarget ? "a" : "");
    }
}
