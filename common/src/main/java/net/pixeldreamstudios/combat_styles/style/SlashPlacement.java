package net.pixeldreamstudios.combat_styles.style;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.bettercombat.api.fx.ParticlePlacement;

import net.pixeldreamstudios.combat_styles.custom.Customs;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public record SlashPlacement(String particleType, float x, float y, float z,
                             float localYaw, float pitch, float roll, int delayTicks,
                             SlashMode mode, Optional<ExtraAttack> attack,
                             SlashAnchor anchor) {

    public static final float LIMIT = 16.0F;
    public static final float ANGLE_LIMIT = 360.0F;
    public static final int MAX_DELAY = 60;

    public static final List<String> TYPES = List.of(
            "", "slash45", "slash90", "slash180", "slash270", "slash360", "stab");

    public static final String DEFAULT_TYPE = "slash90";
    private static final String NO_PARTICLE = "none";
    private static final String KEY_ANCHOR = "anchor";
    private static final String KEY_LEGACY = "follow_hitbox";

    public static final ParticlePlacement FALLBACK =
            new ParticlePlacement(DEFAULT_TYPE, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);

    public static final SlashPlacement DEFAULT =
            new SlashPlacement("", 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0,
                    SlashMode.PARTICLE, Optional.empty(), SlashAnchor.WEAPON);

    public SlashPlacement {
        mode = mode == null ? SlashMode.PARTICLE : mode;
        anchor = anchor == null ? SlashAnchor.WEAPON : anchor;
    }

    private static final Codec<SlashAnchor> ANCHOR_CODEC = Codec.either(
            SlashAnchor.CODEC, Codec.BOOL).xmap(
            either -> either.map(anchor -> anchor,
                    legacy -> legacy ? SlashAnchor.HITBOX : SlashAnchor.WEAPON),
            Either::left);

    private static final MapCodec<SlashAnchor> ANCHOR_FIELD = new MapCodec<>() {

        @Override
        public <T> Stream<T> keys(DynamicOps<T> ops) {
            return Stream.of(ops.createString(KEY_ANCHOR), ops.createString(KEY_LEGACY));
        }

        @Override
        public <T> DataResult<SlashAnchor> decode(DynamicOps<T> ops, MapLike<T> input) {
            T value = input.get(KEY_ANCHOR);
            if (value == null) {
                value = input.get(KEY_LEGACY);
            }
            return value == null
                    ? DataResult.success(SlashAnchor.WEAPON)
                    : ANCHOR_CODEC.parse(ops, value);
        }

        @Override
        public <T> RecordBuilder<T> encode(SlashAnchor input, DynamicOps<T> ops,
                                           RecordBuilder<T> prefix) {
            return input == null || input == SlashAnchor.WEAPON
                    ? prefix
                    : prefix.add(KEY_ANCHOR, ops.createString(input.getSerializedName()));
        }
    };

    public static final Codec<SlashPlacement> CODEC = Customs.wrap(SlashPlacement.class, RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.optionalFieldOf("particle_type", "").forGetter(SlashPlacement::particleType),
            Codec.FLOAT.optionalFieldOf("x", 0.0F).forGetter(SlashPlacement::x),
            Codec.FLOAT.optionalFieldOf("y", 0.0F).forGetter(SlashPlacement::y),
            Codec.FLOAT.optionalFieldOf("z", 0.0F).forGetter(SlashPlacement::z),
            Codec.FLOAT.optionalFieldOf("local_yaw", 0.0F).forGetter(SlashPlacement::localYaw),
            Codec.FLOAT.optionalFieldOf("pitch", 0.0F).forGetter(SlashPlacement::pitch),
            Codec.FLOAT.optionalFieldOf("roll", 0.0F).forGetter(SlashPlacement::roll),
            Codec.INT.optionalFieldOf("delay_ticks", 0).forGetter(SlashPlacement::delayTicks),
            SlashMode.CODEC.optionalFieldOf("mode", SlashMode.PARTICLE).forGetter(SlashPlacement::mode),
            ExtraAttack.CODEC.optionalFieldOf("attack").forGetter(SlashPlacement::attack),
            ANCHOR_FIELD.forGetter(SlashPlacement::anchor)
    ).apply(instance, SlashPlacement::new)));

    public SlashPlacement withParticleType(String value) {
        return new SlashPlacement(value == null ? "" : value, x, y, z,
                localYaw, pitch, roll, delayTicks, mode, attack, anchor);
    }

    public SlashPlacement withMode(SlashMode value) {
        return new SlashPlacement(particleType, x, y, z, localYaw, pitch, roll, delayTicks,
                value == null ? SlashMode.PARTICLE : value, attack, anchor);
    }

    public SlashPlacement withAttack(ExtraAttack value) {
        return new SlashPlacement(particleType, x, y, z, localYaw, pitch, roll, delayTicks,
                mode, Optional.ofNullable(value), anchor);
    }

    public SlashPlacement withField(String field, double value) {
        return switch (field) {
            case "x" -> new SlashPlacement(particleType, (float) value, y, z, localYaw, pitch, roll, delayTicks, mode, attack, anchor);
            case "y" -> new SlashPlacement(particleType, x, (float) value, z, localYaw, pitch, roll, delayTicks, mode, attack, anchor);
            case "z" -> new SlashPlacement(particleType, x, y, (float) value, localYaw, pitch, roll, delayTicks, mode, attack, anchor);
            case "yaw" -> new SlashPlacement(particleType, x, y, z, (float) value, pitch, roll, delayTicks, mode, attack, anchor);
            case "pitch" -> new SlashPlacement(particleType, x, y, z, localYaw, (float) value, roll, delayTicks, mode, attack, anchor);
            case "roll" -> new SlashPlacement(particleType, x, y, z, localYaw, pitch, (float) value, delayTicks, mode, attack, anchor);
            case "delay" -> new SlashPlacement(particleType, x, y, z, localYaw, pitch, roll,
                    Math.clamp((int) Math.round(value), 0, MAX_DELAY), mode, attack, anchor);
            default -> this;
        };
    }

    public ExtraAttack asAttack() {
        ExtraAttack base = attack.orElse(ExtraAttack.DEFAULT);
        return base.withDelay(delayTicks).withAnchor(anchor);
    }

    public SlashPlacement withAnchor(SlashAnchor value) {
        return new SlashPlacement(particleType, x, y, z, localYaw, pitch, roll, delayTicks,
                mode, attack, value == null ? SlashAnchor.WEAPON : value);
    }

    public ParticlePlacement toBetterCombat(ParticlePlacement base) {
        return toBetterCombat(base, HitboxOffset.NONE);
    }

    public ParticlePlacement toBetterCombat(ParticlePlacement base, HitboxOffset offset) {
        String type = particleType == null || particleType.isBlank()
                ? base.particle_type()
                : particleType;
        if (type == null || type.isBlank() || NO_PARTICLE.equals(type)) {
            type = DEFAULT_TYPE;
        }
        HitboxOffset shift = anchor.followsHitbox() && offset != null ? offset : HitboxOffset.NONE;
        return new ParticlePlacement(type,
                base.x_addition() + x + shift.right(),
                base.y_addition() + y + shift.up(),
                base.z_addition() + z + shift.forward(),
                base.local_yaw() + localYaw,
                base.pitch_addition() + pitch,
                roll == 0.0F ? base.roll_set() : roll);
    }

    public String signature() {
        return "p" + particleType + x + "_" + y + "_" + z
                + "_" + localYaw + "_" + pitch + "_" + roll + "_" + delayTicks
                + anchor.getSerializedName();
    }
}
