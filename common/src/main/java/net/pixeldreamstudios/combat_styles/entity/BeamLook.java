package net.pixeldreamstudios.combat_styles.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import net.pixeldreamstudios.combat_styles.affix.AffixEffects;
import net.pixeldreamstudios.combat_styles.custom.Customs;

public record BeamLook(String color, float width, float lift, BeamOrigin origin, int durationTicks,
                       float alpha) {

    public static final String KEY = "beam";
    public static final String KEY_COLOR = "color";
    public static final String KEY_WIDTH = "width";
    public static final String KEY_LIFT = "lift";
    public static final String KEY_ORIGIN = "origin";
    public static final String KEY_DURATION_TICKS = "duration_ticks";
    public static final String KEY_ALPHA = "alpha";
    public static final String DEFAULT_COLOR = "#FF4A3D";
    public static final float DEFAULT_WIDTH = 0.12F;
    public static final float NO_LIFT = 0.0F;
    public static final int DEFAULT_DURATION_TICKS = 6;
    public static final float DEFAULT_ALPHA = 0.85F;
    private static final float MIN_WIDTH = 0.01F;
    private static final int MIN_DURATION = 1;
    private static final float OPAQUE = 1.0F;
    private static final float CLEAR = 0.0F;

    public static final BeamLook DEFAULT = new BeamLook(DEFAULT_COLOR, DEFAULT_WIDTH, NO_LIFT,
            BeamOrigin.ABILITY, DEFAULT_DURATION_TICKS, DEFAULT_ALPHA);

    public static final Codec<BeamLook> CODEC = Customs.wrap(BeamLook.class, RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.optionalFieldOf(KEY_COLOR, DEFAULT_COLOR).forGetter(BeamLook::color),
            Codec.FLOAT.optionalFieldOf(KEY_WIDTH, DEFAULT_WIDTH).forGetter(BeamLook::width),
            Codec.FLOAT.optionalFieldOf(KEY_LIFT, NO_LIFT).forGetter(BeamLook::lift),
            BeamOrigin.CODEC.optionalFieldOf(KEY_ORIGIN, BeamOrigin.ABILITY).forGetter(BeamLook::origin),
            Codec.INT.optionalFieldOf(KEY_DURATION_TICKS, DEFAULT_DURATION_TICKS)
                    .forGetter(BeamLook::durationTicks),
            Codec.FLOAT.optionalFieldOf(KEY_ALPHA, DEFAULT_ALPHA).forGetter(BeamLook::alpha)
    ).apply(instance, BeamLook::new)));

    public BeamLook {
        color = color == null || color.isBlank() ? DEFAULT_COLOR : color;
        width = Math.max(MIN_WIDTH, width);
        origin = origin == null ? BeamOrigin.ABILITY : origin;
        durationTicks = Math.max(MIN_DURATION, durationTicks);
        alpha = Math.clamp(alpha, CLEAR, OPAQUE);
    }

    public int rgb() {
        return new AffixEffects.TrailColour(color, true).argb();
    }

    public Vec3 start(Entity body, Vec3 abilityOrigin) {
        Vec3 base = origin.from(body, abilityOrigin);
        return base.add(0.0D, lift, 0.0D);
    }
}
