package net.pixeldreamstudios.combat_styles.equipment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.pixeldreamstudios.combat_styles.config.AbilityConfig;

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
                             Map<String, AbilityTuning> tuning) {

    public static final Codec<ProfileVariant> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ProfileCondition.LIST_CODEC.optionalFieldOf("conditions", List.of())
                    .forGetter(ProfileVariant::conditions),
            Codec.STRING.optionalFieldOf("finale").forGetter(ProfileVariant::finale),
            Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("swaps", Map.of())
                    .forGetter(ProfileVariant::swaps),
            Codec.STRING.listOf().optionalFieldOf("abilities", List.of())
                    .forGetter(ProfileVariant::abilities),
            Codec.INT.optionalFieldOf("max_active").forGetter(ProfileVariant::maxActive),
            Codec.INT.optionalFieldOf("anchored").forGetter(ProfileVariant::anchored),
            Codec.FLOAT.optionalFieldOf("damage_multiplier")
                    .forGetter(ProfileVariant::damageMultiplier),
            Codec.unboundedMap(Codec.STRING, AbilityTuning.CODEC)
                    .optionalFieldOf("tuning", Map.of())
                    .forGetter(ProfileVariant::tuning)
    ).apply(instance, ProfileVariant::new));

    public ProfileVariant {
        conditions = conditions == null ? List.of() : List.copyOf(conditions);
        swaps = AbilityConfig.keyed(swaps);
        abilities = abilities == null ? List.of() : List.copyOf(abilities);
        tuning = AbilityConfig.keyed(tuning);
    }
}
