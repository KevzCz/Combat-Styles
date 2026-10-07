package net.pixeldreamstudios.combat_styles.equipment;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Optional;

public record AbilityUse(String ability, Optional<AbilityTuning> tuning) {

    public static final String KEY_ABILITY = "ability";
    public static final String KEY_TUNING = "tuning";

    private static final Codec<AbilityUse> FULL = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf(KEY_ABILITY).forGetter(AbilityUse::ability),
            AbilityTuning.CODEC.optionalFieldOf(KEY_TUNING).forGetter(AbilityUse::tuning)
    ).apply(instance, AbilityUse::new));

    public static final Codec<AbilityUse> CODEC = Codec.either(Codec.STRING, FULL).xmap(
            either -> either.map(AbilityUse::of, full -> full),
            Either::right);

    public static final AbilityUse SAMPLE = of("");

    public AbilityUse {
        ability = ability == null ? "" : ability;
        tuning = tuning == null ? Optional.empty() : tuning;
    }

    public static AbilityUse of(String ability) {
        return new AbilityUse(ability, Optional.empty());
    }
}
