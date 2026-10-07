package net.pixeldreamstudios.combat_styles.entity;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;

public enum BeamOrigin implements StringRepresentable {
    ABILITY,
    EYES,
    CENTER,
    FEET;

    public static final Codec<BeamOrigin> CODEC = StringRepresentable.fromEnum(BeamOrigin::values);
    private static final double HALF = 0.5D;

    public Vec3 from(Entity body, Vec3 abilityOrigin) {
        return switch (this) {
            case ABILITY -> abilityOrigin == null ? body.getEyePosition() : abilityOrigin;
            case EYES -> body.getEyePosition();
            case CENTER -> body.position().add(0.0D, body.getBbHeight() * HALF, 0.0D);
            case FEET -> body.position();
        };
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
