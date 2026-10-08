package net.pixeldreamstudios.combat_styles.equipment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.pixeldreamstudios.combat_styles.config.AbilityConfig;
import net.pixeldreamstudios.combat_styles.custom.Customs;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public record ProfileVariant(List<ProfileCondition> conditions,
                             Optional<String> finale,
                             Map<String, String> swaps,
                             List<String> abilities,
                             Optional<Integer> maxActive,
                             Optional<Integer> anchored,
                             Optional<Float> damageMultiplier,
                             Map<String, AbilityTuning> tuning,
                             SummonCadence cadence) {

    public static final Codec<ProfileVariant> CODEC = Customs.wrap(ProfileVariant.class, RecordCodecBuilder.create(instance -> instance.group(
            ProfileCondition.LIST_CODEC.optionalFieldOf("conditions", List.of())
                    .forGetter(ProfileVariant::conditions),
            Codec.STRING.optionalFieldOf("finale").forGetter(ProfileVariant::finale),
            Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("swaps", Map.of())
                    .forGetter(ProfileVariant::swaps),
            Codec.STRING.listOf().optionalFieldOf("abilities", List.of())
                    .forGetter(variant -> List.of()),
            Codec.INT.optionalFieldOf("max_active").forGetter(ProfileVariant::maxActive),
            Codec.INT.optionalFieldOf("anchored").forGetter(ProfileVariant::anchored),
            Codec.FLOAT.optionalFieldOf("damage_multiplier")
                    .forGetter(ProfileVariant::damageMultiplier),
            Codec.unboundedMap(Codec.STRING, AbilityTuning.CODEC)
                    .optionalFieldOf("tuning", Map.of())
                    .forGetter(ProfileVariant::tuning),
            SummonCadence.MAP_CODEC.forGetter(variant -> variant.cadence().carrying(variant.abilities()))
    ).apply(instance, ProfileVariant::new)));

    public ProfileVariant(List<ProfileCondition> conditions, Optional<String> finale,
                          Map<String, String> swaps, List<String> abilities,
                          Optional<Integer> maxActive, Optional<Integer> anchored,
                          Optional<Float> damageMultiplier, Map<String, AbilityTuning> tuning) {
        this(conditions, finale, swaps, abilities, maxActive, anchored, damageMultiplier, tuning,
                SummonCadence.NONE);
    }

    public ProfileVariant {
        conditions = conditions == null ? List.of() : List.copyOf(conditions);
        swaps = AbilityConfig.keyed(swaps);
        cadence = cadence == null ? SummonCadence.NONE : cadence;
        abilities = List.copyOf(cadence.abilitiesOr(abilities == null ? List.of() : abilities));
        cadence = cadence.carrying(List.of());
        tuning = AbilityConfig.keyed(tuning);
    }
}
