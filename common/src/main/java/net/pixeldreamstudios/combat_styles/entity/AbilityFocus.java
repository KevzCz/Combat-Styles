package net.pixeldreamstudios.combat_styles.entity;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;

public enum AbilityFocus implements StringRepresentable {

    TARGET,
    LOOK,
    SELF,
    OWNER,
    AHEAD;

    public static final Codec<AbilityFocus> CODEC = StringRepresentable.fromEnum(AbilityFocus::values);

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public Vec3 pointFor(AbilityHost host, double range) {
        Vec3 origin = host.origin();
        Player owner = host.owner();
        double reach = Math.max(1.0D, range);
        Vec3 ahead = origin.add(host.body().getLookAngle().scale(reach));
        return switch (this) {
            case TARGET -> {
                LivingEntity target = host.target();
                yield target != null && target.isAlive()
                        ? target.getBoundingBox().getCenter()
                        : lookPoint(owner, reach, ahead);
            }
            case LOOK -> lookPoint(owner, reach, ahead);
            case SELF -> origin;
            case OWNER -> owner == null ? origin : owner.getEyePosition();
            case AHEAD -> ahead;
        };
    }

    private static Vec3 lookPoint(Player owner, double reach, Vec3 fallback) {
        return owner == null
                ? fallback
                : owner.getEyePosition().add(owner.getLookAngle().scale(reach));
    }
}
