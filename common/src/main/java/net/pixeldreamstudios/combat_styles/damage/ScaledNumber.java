package net.pixeldreamstudios.combat_styles.damage;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.Optional;

public record ScaledNumber(float base, List<DamageSpec.ScalingEntry> by, DamageSpec.ScalingMode mode,
                           Optional<Float> min, Optional<Float> max, Optional<Float> step) {

    public static final String KEY = "scaled";
    public static final String KEY_BY = "by";
    public static final String KEY_MODE = "scaling_mode";
    public static final String KEY_MIN = "min";
    public static final String KEY_MAX = "max";
    public static final String KEY_STEP = "step";
    public static final String SCHEMA = "@scaled_number";
    private static final float NO_STEP = 0.0F;

    public static final Codec<ScaledNumber> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.fieldOf(KEY).forGetter(ScaledNumber::base),
            DamageSpec.ScalingEntry.CODEC.listOf().optionalFieldOf(KEY_BY, List.of())
                    .forGetter(ScaledNumber::by),
            DamageSpec.ScalingMode.CODEC.optionalFieldOf(KEY_MODE, DamageSpec.ScalingMode.SUM)
                    .forGetter(ScaledNumber::mode),
            Codec.FLOAT.optionalFieldOf(KEY_MIN).forGetter(ScaledNumber::min),
            Codec.FLOAT.optionalFieldOf(KEY_MAX).forGetter(ScaledNumber::max),
            Codec.FLOAT.optionalFieldOf(KEY_STEP).forGetter(ScaledNumber::step)
    ).apply(instance, ScaledNumber::new));

    public ScaledNumber {
        by = by == null ? List.of() : List.copyOf(by);
        mode = mode == null ? DamageSpec.ScalingMode.SUM : mode;
        min = min == null ? Optional.empty() : min;
        max = max == null ? Optional.empty() : max;
        step = step == null ? Optional.empty() : step;
    }

    public static ScaledNumber of(float base, List<DamageSpec.ScalingEntry> by) {
        return new ScaledNumber(base, by, DamageSpec.ScalingMode.SUM, Optional.empty(),
                Optional.empty(), Optional.empty());
    }

    public static boolean isScaled(JsonElement element) {
        return element != null && element.isJsonObject() && element.getAsJsonObject().has(KEY)
                && element.getAsJsonObject().get(KEY).isJsonPrimitive()
                && element.getAsJsonObject().get(KEY).getAsJsonPrimitive().isNumber();
    }

    public static Optional<ScaledNumber> parse(JsonElement element) {
        return isScaled(element)
                ? CODEC.parse(JsonOps.INSTANCE, element).result()
                : Optional.empty();
    }

    public float resolve(Player player) {
        if (player == null || by.isEmpty()) {
            return clamped(base);
        }
        float bonus = 0.0F;
        for (DamageSpec.ScalingEntry entry : by) {
            float part = entry.bonusFor(player, base);
            bonus = mode == DamageSpec.ScalingMode.STRONGEST ? Math.max(bonus, part) : bonus + part;
        }
        float stepSize = step.orElse(NO_STEP);
        if (stepSize > NO_STEP) {
            bonus = (float) (Math.floor(bonus / stepSize) * stepSize);
        }
        return clamped(base + bonus);
    }

    private float clamped(float value) {
        float low = min.map(bound -> Math.max(value, bound)).orElse(value);
        return max.map(bound -> Math.min(low, bound)).orElse(low);
    }

    public List<String> sources() {
        return by.stream().map(DamageSpec.ScalingEntry::source).distinct().toList();
    }
}
