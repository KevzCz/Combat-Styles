package net.pixeldreamstudios.combat_styles.style;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.pixeldreamstudios.combat_styles.custom.Customs;

public record HitboxOffset(float forward, float up, float right,
                           float yaw, float pitch, float roll) {

    public static final float LIMIT = 32.0F;
    public static final float ANGLE_LIMIT = 180.0F;

    public static final HitboxOffset NONE =
            new HitboxOffset(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);

    public HitboxOffset(float forward, float up, float right) {
        this(forward, up, right, 0.0F, 0.0F, 0.0F);
    }

    public static final Codec<HitboxOffset> CODEC = Customs.wrap(HitboxOffset.class, RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.optionalFieldOf("forward", 0.0F).forGetter(HitboxOffset::forward),
            Codec.FLOAT.optionalFieldOf("up", 0.0F).forGetter(HitboxOffset::up),
            Codec.FLOAT.optionalFieldOf("right", 0.0F).forGetter(HitboxOffset::right),
            Codec.FLOAT.optionalFieldOf("yaw", 0.0F).forGetter(HitboxOffset::yaw),
            Codec.FLOAT.optionalFieldOf("pitch", 0.0F).forGetter(HitboxOffset::pitch),
            Codec.FLOAT.optionalFieldOf("roll", 0.0F).forGetter(HitboxOffset::roll)
    ).apply(instance, HitboxOffset::new)));

    public boolean isEmpty() {
        return forward == 0.0F && up == 0.0F && right == 0.0F && !rotates();
    }

    public boolean rotates() {
        return yaw != 0.0F || pitch != 0.0F || roll != 0.0F;
    }

    public boolean reorients() {
        return rotates();
    }

    public HitboxOffset clamped() {
        return new HitboxOffset(
                Math.clamp(forward, -LIMIT, LIMIT),
                Math.clamp(up, -LIMIT, LIMIT),
                Math.clamp(right, -LIMIT, LIMIT),
                Math.clamp(yaw, -ANGLE_LIMIT, ANGLE_LIMIT),
                Math.clamp(pitch, -ANGLE_LIMIT, ANGLE_LIMIT),
                Math.clamp(roll, -ANGLE_LIMIT, ANGLE_LIMIT));
    }

    public String signature() {
        if (isEmpty()) {
            return "";
        }
        return "o" + forward + "_" + up + "_" + right
                + "_" + yaw + "_" + pitch + "_" + roll;
    }
}
