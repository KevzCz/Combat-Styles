package net.pixeldreamstudios.combat_styles.affix;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public record ProjectileVelocity(float gravity, float drag, float acceleration,
                                 float maxSpeed, float divergence, float homing) {

    public static final float INHERIT_GRAVITY = -1.0F;
    public static final float NO_LIMIT = 0.0F;
    public static final float DRAG_LIMIT = 1.0F;
    public static final float ACCEL_LIMIT = 4.0F;
    public static final float SPEED_LIMIT = 16.0F;
    public static final float DIVERGENCE_LIMIT = 45.0F;

    public static final ProjectileVelocity DEFAULT = new ProjectileVelocity(
            INHERIT_GRAVITY, 0.0F, 0.0F, NO_LIMIT, 0.0F, 0.0F);

    public static final Codec<ProjectileVelocity> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    Codec.FLOAT.optionalFieldOf("gravity", INHERIT_GRAVITY)
                            .forGetter(ProjectileVelocity::gravity),
                    Codec.FLOAT.optionalFieldOf("drag", 0.0F)
                            .forGetter(ProjectileVelocity::drag),
                    Codec.FLOAT.optionalFieldOf("acceleration", 0.0F)
                            .forGetter(ProjectileVelocity::acceleration),
                    Codec.FLOAT.optionalFieldOf("max_speed", NO_LIMIT)
                            .forGetter(ProjectileVelocity::maxSpeed),
                    Codec.FLOAT.optionalFieldOf("divergence", 0.0F)
                            .forGetter(ProjectileVelocity::divergence),
                    Codec.FLOAT.optionalFieldOf("homing", 0.0F)
                            .forGetter(ProjectileVelocity::homing)
            ).apply(instance, ProjectileVelocity::new));

    public ProjectileVelocity {
        gravity = Math.clamp(gravity, INHERIT_GRAVITY, ACCEL_LIMIT);
        drag = Math.clamp(drag, -DRAG_LIMIT, DRAG_LIMIT);
        acceleration = Math.clamp(acceleration, -ACCEL_LIMIT, ACCEL_LIMIT);
        maxSpeed = Math.clamp(maxSpeed, NO_LIMIT, SPEED_LIMIT);
        divergence = Math.clamp(divergence, 0.0F, DIVERGENCE_LIMIT);
        homing = Math.clamp(homing, 0.0F, 1.0F);
    }

    public boolean isEmpty() {
        return gravity == INHERIT_GRAVITY && drag == 0.0F && acceleration == 0.0F
                && maxSpeed == NO_LIMIT && divergence == 0.0F && homing == 0.0F;
    }

    public boolean overridesGravity() {
        return gravity > INHERIT_GRAVITY;
    }

    public boolean steersInFlight() {
        return drag != 0.0F || acceleration != 0.0F || homing > 0.0F || overridesGravity();
    }

    public void applyTo(Entity entity, Entity target) {
        if (entity == null || !steersInFlight()) {
            return;
        }
        Vec3 motion = entity.getDeltaMovement();
        if (acceleration != 0.0F) {
            double length = motion.length();
            if (length > 1.0E-4D) {
                motion = motion.add(motion.scale(acceleration / length));
            }
        }
        if (drag != 0.0F) {
            motion = motion.scale(1.0D - drag);
        }
        if (overridesGravity()) {
            motion = motion.subtract(0.0D, gravity, 0.0D);
        }
        if (homing > 0.0F && target != null && target.isAlive()) {
            Vec3 toward = target.position()
                    .add(0.0D, target.getBbHeight() * 0.5D, 0.0D)
                    .subtract(entity.position());
            if (toward.lengthSqr() > 1.0E-4D) {
                double speed = motion.length();
                motion = motion.normalize().scale(1.0D - homing)
                        .add(toward.normalize().scale(homing))
                        .normalize().scale(speed);
            }
        }
        if (maxSpeed > NO_LIMIT) {
            double length = motion.length();
            if (length > maxSpeed) {
                motion = motion.scale(maxSpeed / length);
            }
        }
        entity.setDeltaMovement(motion);
        entity.hasImpulse = true;
    }

    public String signature() {
        return isEmpty() ? "" : "v" + gravity + "_" + drag + "_" + acceleration
                + "_" + maxSpeed + "_" + divergence + "_" + homing;
    }
}
