package net.pixeldreamstudios.combat_styles.equipment;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record EffectCooldown(int ticks, int uses, int duration, boolean refreshOnUse,
                             boolean holdWhileActive) {

    public static final int UNLIMITED = 0;
    public static final int DEFAULT_DURATION = 60;

    public static final EffectCooldown NONE = new EffectCooldown(0, UNLIMITED, 0, true, true);

    public static final Codec<EffectCooldown> EXPANDED = RecordCodecBuilder.create(
            instance -> instance.group(
                    Codec.INT.optionalFieldOf("ticks", 0).forGetter(EffectCooldown::ticks),
                    Codec.INT.optionalFieldOf("uses", UNLIMITED).forGetter(EffectCooldown::uses),
                    Codec.INT.optionalFieldOf("duration", 0).forGetter(EffectCooldown::duration),
                    Codec.BOOL.optionalFieldOf("refresh_on_use", Boolean.TRUE)
                            .forGetter(EffectCooldown::refreshOnUse),
                    Codec.BOOL.optionalFieldOf("hold_while_active", Boolean.TRUE)
                            .forGetter(EffectCooldown::holdWhileActive)
            ).apply(instance, EffectCooldown::new));

    public static final Codec<EffectCooldown> CODEC = Codec.either(Codec.INT, EXPANDED)
            .xmap(either -> either.map(EffectCooldown::of, value -> value),
                    value -> value.windowed() || !value.holdWhileActive()
                            ? Either.right(value)
                            : Either.left(value.ticks()));

    public static final EffectCooldown SAMPLE =
            new EffectCooldown(0, 1, DEFAULT_DURATION, true, true);

    public static EffectCooldown of(int ticks) {
        return new EffectCooldown(ticks, UNLIMITED, 0, true, true);
    }

    public EffectCooldown withTicks(int value) {
        return new EffectCooldown(value, uses, duration, refreshOnUse, holdWhileActive);
    }

    public boolean windowed() {
        return uses > UNLIMITED || duration > 0;
    }

    public boolean limited() {
        return uses > UNLIMITED;
    }

    public int span() {
        return duration <= 0 ? DEFAULT_DURATION : duration;
    }

    public boolean isEmpty() {
        return ticks <= 0 && !windowed() && holdWhileActive;
    }
}
